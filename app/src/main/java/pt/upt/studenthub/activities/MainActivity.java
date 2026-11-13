package pt.upt.studenthub.activities;

import pt.upt.studenthub.R;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/** Ecrã de entrada: valida o nome e abre os três módulos do Student Hub. */
public class MainActivity extends AppCompatActivity {

    private EditText etName;

    /** Liga as Views e configura os eventos na criação do ecrã. */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableStrictModeInDebug();
        setContentView(R.layout.activity_main);

        etName = findViewById(R.id.etName);
        Button btnOpenTimetable = findViewById(R.id.btnOpenTimetable);
        Button btnOpenWeather = findViewById(R.id.btnOpenWeather);

        // MÃ³dulo 1: horÃ¡rio | MÃ³dulo 2: meteorologia do campus
        btnOpenTimetable.setOnClickListener(v -> openScreen(TimetableActivity.class));
        btnOpenWeather.setOnClickListener(v -> openScreen(WeatherActivity.class));
        findViewById(R.id.btnOpenExplorer).setOnClickListener(v -> openScreen(ExplorerActivity.class));
    }

    /**
     * Abre o ecrÃ£ indicado, passando o nome como extra do Intent.
     * Class<?> deixa receber qualquer Activity, assim os dois botÃµes usam o mesmo cÃ³digo.
     */
    private void openScreen(Class<?> destination) {
        String name = getValidatedName();
        if (name == null) {
            return; // nome invÃ¡lido: o erro jÃ¡ estÃ¡ a ser mostrado no campo
        }
        Intent intent = new Intent(this, destination);
        intent.putExtra(TimetableActivity.EXTRA_STUDENT_NAME, name);
        startActivity(intent);
    }

    /** Devolve o nome sem espaÃ§os nas pontas, ou null (com erro no campo) se estiver vazio. */
    @Nullable
    private String getValidatedName() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError(getString(R.string.error_name_required));
            return null;
        }
        return name;
    }

    /**
     * StrictMode sÃ³ em debug. Como esta polÃ­tica substitui a que vem por defeito,
     * temos de voltar a pÃ´r penaltyDeathOnNetwork(), senÃ£o a rede na thread principal deixava de ser bloqueada.
     */
    private void enableStrictModeInDebug() {
        boolean debuggable =
                (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (debuggable) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .detectCustomSlowCalls()
                    .penaltyLog()
                    // Semana 7: usar a rede na thread principal lanÃ§a NetworkOnMainThreadException
                    .penaltyDeathOnNetwork()
                    .build());
        }
    }

    /** Cria o menu a partir do recurso XML. */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /** Trata a opção escolhida pelo utilizador. */

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            AboutDialog.show(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
