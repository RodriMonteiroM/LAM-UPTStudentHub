package pt.upt.studenthub;

import pt.upt.studenthub.activities.DetailActivity;
import pt.upt.studenthub.activities.ExplorerActivity;
import pt.upt.studenthub.activities.SettingsActivity;
import pt.upt.studenthub.activities.TimetableActivity;
import pt.upt.studenthub.models.Beach;
import pt.upt.studenthub.models.ExplorerSettings;
import pt.upt.studenthub.models.MarineItem;
import pt.upt.studenthub.viewmodels.ExplorerViewModel;


import android.app.Activity;
import android.content.Intent;
import android.widget.EditText;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Verifica integração real das Activities, sem substituir o código da aplicação. */
@RunWith(AndroidJUnit4.class)
public class ExplorerScreenTest {
    /** A rotação/recriação conserva a mesma instância do ViewModel e as escolhas. */
    @Test public void explorerSurvivesRecreation() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ExplorerActivity.class)
                .putExtra(TimetableActivity.EXTRA_STUDENT_NAME, "Rodrigo");
        AtomicReference<ExplorerViewModel> original = new AtomicReference<>();
        try (ActivityScenario<ExplorerActivity> screen = ActivityScenario.launch(intent)) {
            screen.onActivity(a -> {
                ExplorerViewModel vm = new ViewModelProvider(a).get(ExplorerViewModel.class);
                original.set(vm); vm.setFavorite("MATOSINHOS|2026-11-11T00:00", true);
            });
            screen.recreate();
            screen.onActivity(a -> {
                ExplorerViewModel vm = new ViewModelProvider(a).get(ExplorerViewModel.class);
                assertSame(original.get(), vm);
                assertTrue(vm.isFavorite("MATOSINHOS|2026-11-11T00:00"));
                assertNotNull(a.findViewById(R.id.exList));
            });
        }
    }
    /** Um valor inválido mantém o formulário aberto e apresenta erro no campo. */
    @Test public void settingsRejectInvalidThreshold() {
        try (ActivityScenario<SettingsActivity> screen = ActivityScenario.launch(SettingsActivity.class)) {
            screen.onActivity(a -> {
                EditText field = a.findViewById(R.id.settingThreshold); field.setText("0");
                a.findViewById(R.id.btnSaveSettings).performClick();
                assertNotNull(field.getError()); assertFalse(a.isFinishing());
            });
        }
    }
    /** As definições válidas são efetivamente devolvidas por setResult. */
    @Test public void settingsReturnValidatedChoices() {
        try (ActivityScenario<SettingsActivity> screen = ActivityScenario.launchActivityForResult(SettingsActivity.class)) {
            screen.onActivity(a -> {
                ((EditText)a.findViewById(R.id.settingThreshold)).setText("2,5");
                ((EditText)a.findViewById(R.id.settingRefresh)).setText("60");
                a.findViewById(R.id.btnSaveSettings).performClick();
            });
            assertEquals(Activity.RESULT_OK, screen.getResult().getResultCode());
            assertEquals(2.5, ExplorerSettings.from(screen.getResult().getResultData()).threshold, 0.001);
        }
    }
    /** Marcar favorito no detalhe devolve a chave da linha correta, mesmo com a rede em curso. */
    @Test public void detailReturnsFavorite() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), DetailActivity.class);
        MarineItem item = new MarineItem(Beach.MATOSINHOS, "2026-11-11T00:00", 1, 270, 9, 17);
        item.put(intent);
        try (ActivityScenario<DetailActivity> screen = ActivityScenario.launchActivityForResult(intent)) {
            screen.onActivity(a -> { a.findViewById(R.id.btnFavorite).performClick(); a.finish(); });
            assertEquals(Activity.RESULT_OK, screen.getResult().getResultCode());
            assertEquals(item.key(), screen.getResult().getResultData().getStringExtra("key"));
            assertTrue(screen.getResult().getResultData().getBooleanExtra("favorite", false));
        }
    }
}
