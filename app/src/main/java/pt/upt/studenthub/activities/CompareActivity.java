package pt.upt.studenthub.activities;

import pt.upt.studenthub.R;
import pt.upt.studenthub.utils.ExplorerUi;
import pt.upt.studenthub.viewmodels.CompareViewModel;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

/** Três cartões lado a lado, com deslocamento horizontal em ecrãs pequenos. */
public class CompareActivity extends AppCompatActivity {

    /** Observa resultados independentes: uma falha deixa as outras colunas visíveis. */
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_compare);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        CompareViewModel vm = new ViewModelProvider(this).get(CompareViewModel.class);
        TextView[] cards = {findViewById(R.id.compareOne), findViewById(R.id.compareTwo), findViewById(R.id.compareThree)};
        vm.getResults().observe(this, results -> {
            for (int i = 0; i < results.size(); i++) {
                CompareViewModel.Result r = results.get(i);
                if (r.item != null) cards[i].setText(ExplorerUi.marine(this, r.item));
                else cards[i].setText(getString(R.string.ex_compare_status, getString(r.beach.nameRes),
                        r.error == null ? getString(R.string.ex_loading) : ExplorerUi.error(this, r.error)));
            }
        });
        vm.load();
    }

    /** Regressa ao Explorer sem criar outra instância. */
    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
