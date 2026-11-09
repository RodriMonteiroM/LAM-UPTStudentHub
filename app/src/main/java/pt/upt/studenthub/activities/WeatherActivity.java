package pt.upt.studenthub.activities;

import pt.upt.studenthub.R;
import pt.upt.studenthub.models.WeatherReport;
import pt.upt.studenthub.network.HttpClient;
import pt.upt.studenthub.network.WeatherApi;
import pt.upt.studenthub.utils.WeatherCodes;
import pt.upt.studenthub.viewmodels.WeatherViewModel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import java.io.IOException;

/**
 * Módulo 2 — Meteorologia do campus (rede + broadcast receivers).
 * Esta Activity só trata da interface; o pedido HTTP vive no WeatherViewModel.
 */
public class WeatherActivity extends AppCompatActivity {

    private WeatherViewModel viewModel;
    private ConnectivityManager connectivityManager;

    private TextView tvOffline;
    private TextView tvStatus;
    private TextView tvTemperature;
    private TextView tvDescription;
    private TextView tvHumidity;
    private TextView tvWind;
    private View weatherCard;
    private ProgressBar progressLoading;
    private Button btnRefresh;
    private CheckBox cbAutoRefresh;

    // Último estado de rede conhecido (serve para perceber quando a rede "volta")
    private boolean online = true;

    /**
     * O sistema envia um broadcast sempre que o modo de avião muda.
     * O extra "state" diz se ficou ligado (true) ou desligado (false).
     * onReceive corre na thread PRINCIPAL: aqui só se mostra um Toast, nada demorado.
     */
    private final BroadcastReceiver airplaneModeReceiver = new BroadcastReceiver() {

    /** Trata o evento recebido do sistema. */
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean enabled = intent.getBooleanExtra("state", false);
            Toast.makeText(context,
                    enabled ? R.string.toast_airplane_on : R.string.toast_airplane_off,
                    Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * Avisa quando a rede por defeito aparece ou desaparece
     * (substitui o broadcast CONNECTIVITY_ACTION, que está descontinuado).
     * ATENÇÃO: estes métodos correm numa thread de FUNDO, por isso usamos runOnUiThread
     * antes de mexer nas Views (senão dá CalledFromWrongThreadException).
     */
    private final ConnectivityManager.NetworkCallback networkCallback =
            new ConnectivityManager.NetworkCallback() {

    /** Notifica a interface de que a rede ficou disponível. */
                @Override
                public void onAvailable(@NonNull Network network) {
                    runOnUiThread(() -> setOnline(true));
                }

    /** Notifica a interface de que a rede foi perdida. */

                @Override
                public void onLost(@NonNull Network network) {
                    runOnUiThread(() -> setOnline(false));
                }
            };

    /** Liga as Views e configura os eventos na criação do ecrã. */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather); // layout ou layout-land

        // Seta Up (o pai está declarado no manifest)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String name = getIntent().getStringExtra(TimetableActivity.EXTRA_STUDENT_NAME);
        if (name == null) {
            name = "";
        }
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        tvGreeting.setText(getString(R.string.weather_title, name));

        tvOffline = findViewById(R.id.tvOffline);
        tvStatus = findViewById(R.id.tvWeatherStatus);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvDescription = findViewById(R.id.tvDescription);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvWind = findViewById(R.id.tvWind);
        weatherCard = findViewById(R.id.weatherCard);
        progressLoading = findViewById(R.id.progressLoading);
        btnRefresh = findViewById(R.id.btnRefresh);
        cbAutoRefresh = findViewById(R.id.cbAutoRefresh);

        connectivityManager = ContextCompat.getSystemService(this, ConnectivityManager.class);

        // Depois de rodar o ecrã devolve o MESMO ViewModel (com o pedido que já estava a correr)
        viewModel = new ViewModelProvider(this).get(WeatherViewModel.class);

        btnRefresh.setOnClickListener(v -> viewModel.load(WeatherApi.currentWeatherUrl()));

        viewModel.getReport().observe(this, this::showReport);
        viewModel.getStatus().observe(this, this::showStatus);

        // Só na primeira abertura (IDLE) é que os dados são descarregados automaticamente.
        // Após uma rotação o estado já é LOADING/SUCCESS/ERROR, logo não há segundo pedido.
        if (viewModel.getStatus().getValue() == WeatherViewModel.Status.IDLE) {
            viewModel.load(WeatherApi.currentWeatherUrl());
        }
    }

    // ---------- Registar / anular registo conforme o ciclo de vida ----------

    /** Regista o recetor do modo de avião e o callback de rede enquanto o ecrã está visível. */
    @Override
    protected void onStart() {
        super.onStart();
        // Android 14+: é obrigatório dizer se outras apps podem enviar para este receiver
        ContextCompat.registerReceiver(this, airplaneModeReceiver,
                new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED);

        setOnline(isOnlineNow());                                            // estado atual
        connectivityManager.registerDefaultNetworkCallback(networkCallback); // mudanças futuras
    }

    /** Liberta os registos antes de o ecrã deixar de estar visível. */

    @Override
    protected void onStop() {
        // Sem isto aparecia "has leaked IntentReceiver" no Logcat
        unregisterReceiver(airplaneModeReceiver);
        connectivityManager.unregisterNetworkCallback(networkCallback);
        super.onStop();
    }

    // ---------- Estado da rede ----------

    /** Verifica se, neste momento, existe uma rede ativa com acesso à Internet. */
    private boolean isOnlineNow() {
        Network active = connectivityManager.getActiveNetwork();
        if (active == null) {
            return false;
        }
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(active);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    /**
     * Atualiza o aviso de offline. Se a rede acabou de VOLTAR (estava offline e agora não),
     * a CheckBox está marcada e o último pedido falhou, repete o pedido automaticamente.
     */
    private void setOnline(boolean nowOnline) {
        boolean cameBack = !online && nowOnline;
        online = nowOnline;
        tvOffline.setVisibility(online ? View.GONE : View.VISIBLE);

        if (cameBack && cbAutoRefresh.isChecked()
                && viewModel.getStatus().getValue() == WeatherViewModel.Status.ERROR) {
            Toast.makeText(this, R.string.toast_back_online, Toast.LENGTH_SHORT).show();
            viewModel.load(WeatherApi.currentWeatherUrl());
        }
    }

    // ---------- Mostrar dados no ecrã ----------

    /** Liga/desliga spinner e botão conforme o estado e escreve a mensagem certa. */
    private void showStatus(WeatherViewModel.Status s) {
        boolean loading = s == WeatherViewModel.Status.LOADING;
        progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRefresh.setEnabled(!loading);

        switch (s) {
            case IDLE:
                tvStatus.setText(R.string.status_weather_idle);
                break;
            case LOADING:
                tvStatus.setText(R.string.status_loading);
                break;
            case SUCCESS:
                WeatherReport r = viewModel.getReport().getValue();
                tvStatus.setText(getString(R.string.status_success, r == null ? "" : r.hour()));
                break;
            case ERROR:
                tvStatus.setText(errorToText(viewModel.getError().getValue()));
                break;
        }
        invalidateOptionsMenu(); // obriga o menu a voltar a correr onPrepareOptionsMenu
    }

    /** O ViewModel não tem Context, por isso dá um enum; aqui é que se transforma em texto. */
    private String errorToText(WeatherViewModel.ErrorType type) {
        if (type == null) {
            return getString(R.string.err_no_connection);
        }
        switch (type) {
            case TIMEOUT:
                return getString(R.string.err_timeout);
            case HTTP:
                return getString(R.string.err_http, viewModel.getLastHttpCode());
            case PARSE:
                return getString(R.string.err_parse);
            default:
                return getString(R.string.err_no_connection);
        }
    }

    /** Mostra as medições meteorológicas e a animação do cartão. */

    private void showReport(WeatherReport r) {
        if (r == null) {
            weatherCard.setVisibility(View.GONE);
            return;
        }
        tvTemperature.setText(getString(R.string.temp_value, r.temperature));
        tvDescription.setText(WeatherCodes.describe(r.weatherCode));
        tvHumidity.setText(getString(R.string.humidity_value, r.humidity));
        tvWind.setText(getString(R.string.wind_value, r.windSpeed));

        weatherCard.setVisibility(View.VISIBLE);
        weatherCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.item_enter));
    }

    // ---------- Options Menu ----------

    /** Cria as opções de atualização, demonstração de erros e informação da aplicação. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_weather, menu);
        return true;
    }

    /** Enquanto há um pedido a decorrer, as opções que fazem novo pedido ficam desativadas. */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean loading = viewModel.getStatus().getValue() == WeatherViewModel.Status.LOADING;
        menu.findItem(R.id.action_refresh).setEnabled(!loading);
        menu.findItem(R.id.action_server_error).setEnabled(!loading);
        return super.onPrepareOptionsMenu(menu);
    }

    /** Trata a opção escolhida pelo utilizador. */

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            viewModel.load(WeatherApi.currentWeatherUrl());
            return true;
        } else if (id == R.id.action_server_error) {
            viewModel.load(WeatherApi.brokenUrl());
            return true;
        } else if (id == R.id.action_main_thread) {
            fetchOnMainThreadOnPurpose();
            return true;
        } else if (id == R.id.action_about) {
            AboutDialog.show(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * ✗ MÁ PRÁTICA — só para demonstração.
     * Usa a rede na thread principal de propósito; o StrictMode (penaltyDeathOnNetwork)
     * lança NetworkOnMainThreadException antes de sair qualquer byte. Apanhamos a exceção
     * para a app não ir abaixo e mostramos um Toast.
     */
    private void fetchOnMainThreadOnPurpose() {
        try {
            HttpClient.get(WeatherApi.currentWeatherUrl());
        } catch (NetworkOnMainThreadException e) {
            Toast.makeText(this, R.string.toast_main_thread, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, R.string.err_no_connection, Toast.LENGTH_LONG).show();
        }
    }
}
