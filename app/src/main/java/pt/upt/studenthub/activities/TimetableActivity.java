package pt.upt.studenthub.activities;

import pt.upt.studenthub.R;
import pt.upt.studenthub.viewmodels.TimetableViewModel;

import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

/** Módulo 1: demonstra sincronização assíncrona e estado da interface. */
public class TimetableActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_NAME = "pt.upt.studenthub.STUDENT_NAME";
    private static final String KEY_TAPS = "key_taps";

    private TimetableViewModel viewModel;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private Button btnSync;
    private Button btnCancel;
    private Button btnTap;
    private LinearLayout sessionsContainer;
    private int taps = 0;

    /** Liga as Views e configura os eventos na criação do ecrã. */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable); // layout ou layout-land, conforme a orientação

        // Semana 5: seta Up (o pai está definido no manifest)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Semana 4: lê o extra que veio no Intent
        String name = getIntent().getStringExtra(EXTRA_STUDENT_NAME);
        if (name == null) {
            name = "";
        }
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(getString(R.string.timetable_title, name));

        tvStatus = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);
        btnSync = findViewById(R.id.btnSync);
        btnCancel = findViewById(R.id.btnCancel);
        btnTap = findViewById(R.id.btnTap);
        sessionsContainer = findViewById(R.id.sessionsContainer);

        // Semana 5: pequeno estado da interface recuperado do Bundle
        if (savedInstanceState != null) {
            taps = savedInstanceState.getInt(KEY_TAPS, 0);
        }
        updateTapButton();

        // Semana 6: depois de rodar o ecrã, continua a ser o mesmo ViewModel
        viewModel = new ViewModelProvider(this).get(TimetableViewModel.class);

        btnSync.setOnClickListener(v ->
                viewModel.startSync(getResources().getStringArray(R.array.sessions)));
        btnCancel.setOnClickListener(v -> viewModel.cancelSync());
        btnTap.setOnClickListener(v -> {
            taps++;
            updateTapButton();
        });

        // Observadores: correm na thread principal e só enquanto a Activity está visível
        viewModel.getProgress().observe(this, p -> {
            progressBar.setProgress(p);
            if (viewModel.getState().getValue() == TimetableViewModel.State.RUNNING) {
                tvStatus.setText(getString(R.string.status_running, p));
            }
        });
        viewModel.getSessions().observe(this, this::renderSessions);
        viewModel.getState().observe(this, this::renderState);
    }

    /** Mostra quantos toques foram feitos no botão de demonstração. */
    private void updateTapButton() {
        btnTap.setText(getString(R.string.btn_tap, taps));
    }

    /** Adapta o progresso, os botões e a mensagem ao estado da sincronização. */
    private void renderState(TimetableViewModel.State s) {
        boolean running = s == TimetableViewModel.State.RUNNING;
        btnSync.setEnabled(!running);
        btnCancel.setEnabled(running);

        switch (s) {
            case IDLE:
                tvStatus.setText(R.string.status_idle);
                break;
            case RUNNING:
                Integer p = viewModel.getProgress().getValue();
                tvStatus.setText(getString(R.string.status_running, p == null ? 0 : p));
                break;
            case DONE:
                List<String> items = viewModel.getSessions().getValue();
                tvStatus.setText(getString(R.string.status_done,
                        items == null ? 0 : items.size()));
                break;
            case CANCELLED:
                tvStatus.setText(R.string.status_cancelled);
                break;
        }
        invalidateOptionsMenu(); // atualiza se a opção "Clear" está ativa ou não
    }

    /** Acrescenta só as sessões novas; cada uma entra com uma animação definida em XML. */
    private void renderSessions(List<String> items) {
        if (items.size() < sessionsContainer.getChildCount()) {
            sessionsContainer.removeAllViews(); // a lista foi limpa ou recomeçou
        }
        int pad = getResources().getDimensionPixelSize(R.dimen.spacing_s);
        for (int i = sessionsContainer.getChildCount(); i < items.size(); i++) {
            TextView tv = new TextView(this);
            tv.setText(items.get(i));
            tv.setTextAppearance(android.R.style.TextAppearance_Material_Medium);
            tv.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch_target));
            tv.setPadding(0, pad, 0, pad);
            sessionsContainer.addView(tv);
            tv.startAnimation(AnimationUtils.loadAnimation(this, R.anim.item_enter));
        }
    }

    /** Guarda o estado pequeno da interface antes da recriação. */

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TAPS, taps);
    }

    // ---------- Options Menu ----------

    /** Infla as opções do horário a partir do XML. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_timetable, menu);
        return true;
    }

    /** Atualiza as opções do menu de acordo com o estado atual. */

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem clear = menu.findItem(R.id.action_clear);
        clear.setEnabled(viewModel.getState().getValue()
                != TimetableViewModel.State.RUNNING);
        return super.onPrepareOptionsMenu(menu);
    }

    /** Trata a opção escolhida pelo utilizador. */

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_clear) {
            viewModel.clear();
            return true;
        } else if (id == R.id.action_block_ui) {
            blockUiThreadOnPurpose();
            return true;
        } else if (id == R.id.action_about) {
            AboutDialog.show(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** ✗ MÁ PRÁTICA — só para demonstração (interface presa / ANR / StrictMode). */
    private void blockUiThreadOnPurpose() {
        StrictMode.noteSlowCall("Blocking the UI thread on purpose");
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Toast.makeText(this, R.string.msg_blocked, Toast.LENGTH_SHORT).show();
    }
}
