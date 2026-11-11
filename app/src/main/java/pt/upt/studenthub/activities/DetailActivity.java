package pt.upt.studenthub.activities;

import pt.upt.studenthub.R;
import pt.upt.studenthub.models.MarineItem;
import pt.upt.studenthub.utils.ExplorerUi;
import pt.upt.studenthub.viewmodels.DetailViewModel;
import pt.upt.studenthub.viewmodels.ExplorerViewModel;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

/** Detalhe em retrato: pedido B e devolução do favorito através de setResult. */
public class DetailActivity extends AppCompatActivity {
    private static final String TAG = "DetailLifecycle";
    private DetailViewModel viewModel;
    private TextView detailText;
    private Button favoriteButton, retryButton;

    /** Recebe a linha por extras e só inicia rede se este ViewModel ainda não a carregou. */
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_detail);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);
        detailText = findViewById(R.id.detailText);
        favoriteButton = findViewById(R.id.btnFavorite);
        retryButton = findViewById(R.id.btnRetryDetail);
        viewModel.load(MarineItem.from(getIntent()), getIntent().getBooleanExtra("favorite", false), false);
        viewModel.getStatus().observe(this, status -> render());
        favoriteButton.setOnClickListener(v -> {
            if (viewModel.isFavorite()) {
                new AlertDialog.Builder(this).setMessage(R.string.ex_confirm_remove)
                        .setNegativeButton(R.string.ex_no, null)
                        .setPositiveButton(R.string.ex_yes, (dialog, which) -> change(false)).show();
            } else {
                change(true);
            }
        });
        retryButton.setOnClickListener(v -> viewModel.load(viewModel.getItem(), viewModel.isFavorite(), true));
        returnFavorite();
    }

    /** Apresenta o cartão e permite repetir um pedido falhado. */
    private void render() {
        ExplorerUi.detail(detailText, viewModel);
        favoriteButton.setText(viewModel.isFavorite() ? R.string.ex_unfavorite : R.string.ex_favorite);
        retryButton.setEnabled(viewModel.getStatus().getValue() != ExplorerViewModel.Status.LOADING);
        if (viewModel.getStatus().getValue() == ExplorerViewModel.Status.SUCCESS)
            detailText.startAnimation(AnimationUtils.loadAnimation(this, R.anim.detail_enter));
    }

    /** Atualiza o resultado antes de voltar, tanto pela seta Up como pelo botão do sistema. */
    private void change(boolean isFavorite) {
        viewModel.setFavorite(isFavorite);
        returnFavorite();
        render();
        Toast.makeText(this, isFavorite ? R.string.ex_added : R.string.ex_removed, Toast.LENGTH_SHORT).show();
    }

    /** setResult não fecha a Activity: guarda o resultado a entregar quando esta terminar. */
    private void returnFavorite() {
        setResult(RESULT_OK, new Intent().putExtra("key", viewModel.getItem().key()).putExtra("favorite", viewModel.isFavorite()));
    }

    /** A seta Up termina o detalhe e entrega o resultado. */
    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Regista a entrada no estado visível. */
    @Override protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    /** Regista o início da interação. */
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

    /** Regista a saída do estado visível. */
    @Override protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    /** Regista o fim desta instância (inclui a rotação). */
    @Override protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
}
