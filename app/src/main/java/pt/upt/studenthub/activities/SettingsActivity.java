package pt.upt.studenthub.activities;

import pt.upt.studenthub.R;
import pt.upt.studenthub.models.ExplorerSettings;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/** Formulário da variante 3: valida antes de devolver as quatro definições. */
public class SettingsActivity extends AppCompatActivity {
    private EditText thresholdInput, refreshInput;
    private RadioGroup stepGroup;
    private CheckBox twoDaysCheckBox;

    /** Preenche os valores de entrada só na primeira criação; Android restaura as Views na rotação. */
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_settings);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        thresholdInput = findViewById(R.id.settingThreshold);
        refreshInput = findViewById(R.id.settingRefresh);
        stepGroup = findViewById(R.id.settingStep);
        twoDaysCheckBox = findViewById(R.id.settingDays);
        if (state == null) {
            ExplorerSettings settings = ExplorerSettings.from(getIntent());
            thresholdInput.setText(String.valueOf(settings.threshold));
            refreshInput.setText(String.valueOf(settings.refreshSeconds));
            stepGroup.check(settings.step == 1 ? R.id.stepOne : R.id.stepThree);
            twoDaysCheckBox.setChecked(settings.days == 2);
        }
        findViewById(R.id.btnSaveSettings).setOnClickListener(v -> save());
    }

    /**
     * Converte os campos e verifica os limites: 0,5–5,0 m e 30–3600 s.
     * Só devolve as definições e fecha o formulário quando os dois valores são válidos.
     */
    private void save() {
        double limit;
        int seconds;
        try {
            limit = Double.parseDouble(thresholdInput.getText().toString().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            thresholdInput.setError(getString(R.string.ex_threshold_error));
            return;
        }
        // A conversão pode funcionar e o número continuar fora dos limites permitidos.
        if (!Double.isFinite(limit) || limit < 0.5 || limit > 5.0) {
            thresholdInput.setError(getString(R.string.ex_threshold_error));
            return;
        }
        try {
            seconds = Integer.parseInt(refreshInput.getText().toString().trim());
        } catch (NumberFormatException e) {
            refreshInput.setError(getString(R.string.ex_refresh_error));
            return;
        }
        if (seconds < 30 || seconds > 3600) {
            refreshInput.setError(getString(R.string.ex_refresh_error));
            return;
        }
        // O RadioGroup escolhe 1 h ou 3 h; a CheckBox escolhe dois dias ou apenas um.
        ExplorerSettings choices = new ExplorerSettings(limit, stepGroup.getCheckedRadioButtonId() == R.id.stepOne ? 1 : 3,
                twoDaysCheckBox.isChecked() ? 2 : 1, seconds);
        Intent result = new Intent();
        choices.put(result);
        setResult(RESULT_OK, result);
        finish();
    }

    /** Voltar sem Guardar mantém as definições anteriores. */
    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
