package pt.upt.studenthub.activities;

import pt.upt.studenthub.R;
import pt.upt.studenthub.adapters.ItemAdapter;
import pt.upt.studenthub.models.Beach;
import pt.upt.studenthub.models.ExplorerSettings;
import pt.upt.studenthub.models.MarineItem;
import pt.upt.studenthub.utils.ExplorerUi;
import pt.upt.studenthub.viewmodels.DetailViewModel;
import pt.upt.studenthub.viewmodels.ExplorerViewModel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

/** Lista e eventos do dispositivo; a rede pertence aos ViewModels e nunca à Activity. */
@SuppressWarnings("deprecation") // startActivityForResult foi lecionado na Semana 4 e é permitido no enunciado.
public class ExplorerActivity extends AppCompatActivity {
    private static final String TAG = "ExplorerLifecycle";
    private static final int DETAIL = 10, SETTINGS = 11;
    private ExplorerViewModel viewModel;
    private DetailViewModel detailViewModel;
    private ItemAdapter adapter;
    private ListView itemList;
    private TextView statusText, offlineBanner, batteryWarning, detailText;
    private Button favoriteButton, retryDetailButton;
    private ConnectivityManager connectivity;
    private boolean started, online;
    private Parcelable listPosition;
    private final Handler timer = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        /** Atualiza se há rede e volta a agendar apenas enquanto o ecrã está visível. */
        @Override public void run() {
            if (started && !viewModel.isBatteryLow() && online) viewModel.load();
            schedule();
        }
    };
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        /** Os broadcasts suspendem ou retomam o ciclo sem cancelar um pedido manual. */
        @Override public void onReceive(Context context, Intent intent) {
            viewModel.setBatteryLow(Intent.ACTION_BATTERY_LOW.equals(intent.getAction()));
            renderBattery();
            schedule();
        }
    };
    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        /** Não toca em Views na thread do sistema. */
        @Override public void onAvailable(@NonNull Network network) { runOnUiThread(() -> updateNetwork()); }
        /** Reavalia a rede ativa: outra ligação pode já ter substituído a anterior. */
        @Override public void onLost(@NonNull Network network) { runOnUiThread(() -> updateNetwork()); }
        /** Espera também pela validação da ligação à Internet. */
        @Override public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities capabilities) {
            runOnUiThread(() -> updateNetwork());
        }
    };

    /** Prepara o ecrã, restaura a seleção e inicia a lista apenas se ainda não houve pedido. */
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_explorer);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        viewModel = new ViewModelProvider(this).get(ExplorerViewModel.class);
        detailViewModel = new ViewModelProvider(this).get(DetailViewModel.class);
        connectivity = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        ((TextView)findViewById(R.id.exGreeting)).setText(getString(R.string.ex_greeting,
                getIntent().getStringExtra(TimetableActivity.EXTRA_STUDENT_NAME)));
        bindViews();
        if (state != null) listPosition = state.getParcelable("listPosition");
        setupListeners();
        observeData();
        if (detailText != null && viewModel.getSelected() != null)
            detailViewModel.load(viewModel.getSelected(), viewModel.isFavorite(viewModel.getSelected().key()), false);
        if (viewModel.getStatus().getValue() == ExplorerViewModel.Status.IDLE) viewModel.load();
    }

    /** Liga as Views do XML; os controlos de detalhe só existem em paisagem. */
    private void bindViews() {
        itemList = findViewById(R.id.exList);
        statusText = findViewById(R.id.exStatus);
        offlineBanner = findViewById(R.id.exOffline);
        batteryWarning = findViewById(R.id.exBattery);
        detailText = findViewById(R.id.detailText);
        favoriteButton = findViewById(R.id.btnFavorite);
        retryDetailButton = findViewById(R.id.btnRetryDetail);
        adapter = new ItemAdapter(this);
        itemList.setAdapter(adapter);
    }

    /** Define os cliques da lista e dos botões; as ações do painel só existem em paisagem. */
    private void setupListeners() {
        itemList.setOnItemClickListener((parent, view, position, id) -> openDetail(adapter.getItem(position)));
        findViewById(R.id.exCancel).setOnClickListener(v -> viewModel.cancel());
        if (favoriteButton != null) {
            favoriteButton.setOnClickListener(v -> toggleFavorite());
            retryDetailButton.setOnClickListener(v -> {
                MarineItem selectedItem = detailViewModel.getItem();
                if (selectedItem != null) {
                    // true força uma nova tentativa, mesmo que a hora selecionada seja a mesma.
                    detailViewModel.load(selectedItem, viewModel.isFavorite(selectedItem.key()), true);
                }
            });
        }
    }

    /** Observa lista, favoritos, definições e estados; conserva a posição ao atualizar as linhas. */
    private void observeData() {
        viewModel.getItems().observe(this, items -> {
            // Usa a posição guardada na rotação ou a posição atual antes de substituir as linhas.
            Parcelable position = listPosition != null ? listPosition : itemList.onSaveInstanceState();
            adapter.submit(items);
            itemList.onRestoreInstanceState(position);
            listPosition = null;
            itemList.scheduleLayoutAnimation();
        });
        viewModel.getFavorites().observe(this, keys -> {
            decorate();
            renderDetail();
        });
        viewModel.getSettings().observe(this, choices -> decorate());
        viewModel.getStatus().observe(this, currentStatus -> render());
        detailViewModel.getStatus().observe(this, currentStatus -> renderDetail());
    }

    /** Atualiza os ícones sem alterar a lista nem fazer rede. */
    private void decorate() { adapter.decorate(viewModel.getFavorites().getValue(), viewModel.getSettings().getValue().threshold); }

    /** Mensagens explícitas para os seis estados; conserva os dados existentes em erro. */
    private void render() {
        ExplorerViewModel.Status currentStatus = viewModel.getStatus().getValue();
        int text = R.string.ex_idle;
        if (currentStatus == ExplorerViewModel.Status.LOADING) text = R.string.ex_loading;
        if (currentStatus == ExplorerViewModel.Status.SUCCESS) text = R.string.ex_success;
        if (currentStatus == ExplorerViewModel.Status.EMPTY) text = R.string.ex_empty;
        if (currentStatus == ExplorerViewModel.Status.CANCELLED) text = R.string.ex_cancelled;
        statusText.setText(currentStatus == ExplorerViewModel.Status.ERROR ? ExplorerUi.error(this, viewModel.getError()) : getString(text));
        boolean loading = currentStatus == ExplorerViewModel.Status.LOADING;
        findViewById(R.id.exProgress).setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.exCancel).setEnabled(loading);
        itemList.setEnabled(!loading);
        renderDetail();
        invalidateOptionsMenu();
    }

    /** Em paisagem usa o painel; em retrato abre uma Activity com resultado. */
    private void openDetail(MarineItem item) {
        viewModel.select(item);
        if (detailText != null) {
            detailViewModel.load(item, viewModel.isFavorite(item.key()), false);
            renderDetail();
        } else {
            Intent intent = new Intent(this, DetailActivity.class);
            item.put(intent);
            intent.putExtra("favorite", viewModel.isFavorite(item.key()));
            startActivityForResult(intent, DETAIL);
            overridePendingTransition(R.anim.screen_enter, R.anim.screen_exit);
        }
    }

    /** Atualiza o painel de paisagem com os dados do seu ViewModel e o estado dos botões. */
    private void renderDetail() {
        if (detailText == null) return;
        ExplorerUi.detail(detailText, detailViewModel);
        MarineItem item = detailViewModel.getItem();
        favoriteButton.setEnabled(item != null);
        favoriteButton.setText(item != null && viewModel.isFavorite(item.key()) ? R.string.ex_unfavorite : R.string.ex_favorite);
        retryDetailButton.setEnabled(item != null && detailViewModel.getStatus().getValue() != ExplorerViewModel.Status.LOADING
                && viewModel.getStatus().getValue() != ExplorerViewModel.Status.LOADING);
        if (detailViewModel.getStatus().getValue() == ExplorerViewModel.Status.SUCCESS)
            detailText.startAnimation(AnimationUtils.loadAnimation(this, R.anim.detail_enter));
    }

    /** Remover pede confirmação; adicionar dá feedback imediato. */
    private void toggleFavorite() {
        MarineItem item = detailViewModel.getItem();
        if (item == null) return;
        boolean remove = viewModel.isFavorite(item.key());
        if (remove) {
            new AlertDialog.Builder(this).setMessage(R.string.ex_confirm_remove)
                    .setNegativeButton(R.string.ex_no, null)
                    .setPositiveButton(R.string.ex_yes,
                            (dialog, which) -> changeFavorite(item.key(), false)).show();
        } else {
            changeFavorite(item.key(), true);
        }
    }

    /** Aplica a escolha e informa o utilizador. */
    private void changeFavorite(String key, boolean isFavorite) {
        viewModel.setFavorite(key, isFavorite);
        Toast.makeText(this, isFavorite ? R.string.ex_added : R.string.ex_removed, Toast.LENGTH_SHORT).show();
    }

    /** Recebe as escolhas dos dois ecrãs que devolvem resultados. */
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null) return;
        if (request == DETAIL) viewModel.setFavorite(data.getStringExtra("key"), data.getBooleanExtra("favorite", false));
        if (request == SETTINGS) {
            viewModel.apply(ExplorerSettings.from(data));
            schedule();
            Toast.makeText(this, R.string.ex_settings_saved, Toast.LENGTH_SHORT).show();
        }
    }

    /** Menu com grupo de praias checkable. */
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_explorer, menu);
        return true;
    }

    /** Desativa os comandos que iniciam pedidos enquanto a lista está a carregar. */
    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        boolean enabled = viewModel.getStatus().getValue() != ExplorerViewModel.Status.LOADING;
        for (int id : new int[]{R.id.exRefresh, R.id.exPlaces, R.id.exCompare, R.id.exSettings}) menu.findItem(id).setEnabled(enabled);
        // A ordem corresponde aos valores do enum Beach; ordinal() dá o índice da praia.
        int[] places = {R.id.exMatosinhos, R.id.exPovoa, R.id.exNazare};
        menu.findItem(places[viewModel.getBeach().ordinal()]).setChecked(true);
        return super.onPrepareOptionsMenu(menu);
    }

    /** Encaminha cada ação para o ViewModel ou para o ecrã respetivo. */
    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        if (id == R.id.exAbout) {
            AboutDialog.show(this);
            return true;
        }
        if (viewModel.getStatus().getValue() == ExplorerViewModel.Status.LOADING) return true;
        if (id == R.id.exRefresh) {
            viewModel.load();
            return true;
        }
        Beach beach = beachForMenuItem(id);
        if (beach != null) {
            detailViewModel.clear();
            viewModel.changeBeach(beach);
            return true;
        }
        if (id == R.id.exCompare) {
            startActivity(new Intent(this, CompareActivity.class));
        } else if (id == R.id.exSettings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            viewModel.getSettings().getValue().put(intent);
            startActivityForResult(intent, SETTINGS);
        } else {
            return super.onOptionsItemSelected(item);
        }
        overridePendingTransition(R.anim.screen_enter, R.anim.screen_exit);
        return true;
    }

    /** Associa cada opção de praia ao respetivo modelo; as outras opções devolvem null. */
    private Beach beachForMenuItem(int id) {
        if (id == R.id.exMatosinhos) return Beach.MATOSINHOS;
        if (id == R.id.exPovoa) return Beach.POVOA;
        if (id == R.id.exNazare) return Beach.NAZARE;
        return null;
    }

    /** Substitui o agendamento anterior; só agenda se o ecrã estiver visível e a bateria permitir. */
    private void schedule() {
        timer.removeCallbacks(refresh);
        // postDelayed recebe milissegundos; as definições guardam o intervalo em segundos.
        if (started && !viewModel.isBatteryLow()) timer.postDelayed(refresh, viewModel.getSettings().getValue().refreshSeconds * 1000L);
    }

    /** Mostra o aviso de suspensão. */
    private void renderBattery() { batteryWarning.setVisibility(viewModel.isBatteryLow() ? View.VISIBLE : View.GONE); }

    /** Consulta o estado atual e repete o último pedido falhado quando a rede regressa. */
    private void updateNetwork() {
        if (!started) return;
        NetworkCapabilities capabilities = connectivity.getNetworkCapabilities(connectivity.getActiveNetwork());
        boolean connected = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        boolean networkReturned = connected && !online;
        online = connected;
        offlineBanner.setVisibility(connected ? View.GONE : View.VISIBLE);
        if (networkReturned && viewModel.getStatus().getValue() == ExplorerViewModel.Status.ERROR) viewModel.load();
    }

    /** Regista os eventos só enquanto a Activity está visível. */
    @Override protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        started = true;
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        ContextCompat.registerReceiver(this, batteryReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        // O broadcast BATTERY_CHANGED é sticky: permite detetar bateria já fraca na abertura.
        Intent batteryState = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryState != null) {
            int level = batteryState.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryState.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            boolean plugged = batteryState.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            if (level >= 0 && scale > 0) viewModel.setBatteryLow(!plugged && level * 100 / scale <= 15);
        }
        renderBattery();
        updateNetwork();
        connectivity.registerDefaultNetworkCallback(networkCallback);
        schedule();
    }

    /** Retoma o ecrã e regista o evento para a defesa no Logcat. */
    @Override protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    /** Regista o regresso depois de o ecrã ter estado parado. */
    @Override protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    /** Regista a perda de foco. */
    @Override protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    /** Remove recetor, callback e temporizador para evitar fugas da Activity. */
    @Override protected void onStop() {
        started = false;
        timer.removeCallbacks(refresh);
        connectivity.unregisterNetworkCallback(networkCallback);
        unregisterReceiver(batteryReceiver);
        Log.d(TAG, "onStop");
        super.onStop();
    }

    /** Guarda a posição da ListView, enquanto os dados ficam no ViewModel. */
    @Override protected void onSaveInstanceState(@NonNull Bundle out) {
        out.putParcelable("listPosition", itemList.onSaveInstanceState());
        super.onSaveInstanceState(out);
    }

    /** Regista a destruição; os pedidos sobrevivem se for apenas uma rotação. */
    @Override protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
}
