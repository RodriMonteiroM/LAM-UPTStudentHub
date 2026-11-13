package pt.upt.studenthub.viewmodels;

import pt.upt.studenthub.models.ExplorerError;
import pt.upt.studenthub.models.MarineDetail;
import pt.upt.studenthub.models.MarineItem;
import pt.upt.studenthub.network.ExplorerApi;
import pt.upt.studenthub.network.HttpClient;
import pt.upt.studenthub.parsers.ExplorerParser;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Pedido B independente, reutilizado pelo ecrã de detalhe e pelo painel de paisagem. */
public class DetailViewModel extends ViewModel {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<ExplorerViewModel.Status> status = new MutableLiveData<>(ExplorerViewModel.Status.IDLE);
    private Future<?> task;
    // Cada pedido tem uma versão; cancelar invalida respostas antigas ainda na fila.
    private int requestVersion;
    private MarineItem item;
    private MarineDetail detail;
    private ExplorerError error;
    private boolean favorite;

    /** Permite observar o fim do pedido. */
    public LiveData<ExplorerViewModel.Status> getStatus() { return status; }

    /** Linha cujos dados estão a ser apresentados. */
    public MarineItem getItem() { return item; }

    /** Dados devolvidos pelo pedido B. */
    public MarineDetail getDetail() { return detail; }

    /** Erro do último pedido. */
    public ExplorerError getError() { return error; }

    /** Favorito conservado na rotação da DetailActivity. */
    public boolean isFavorite() { return favorite; }

    /** Guarda a escolha local; a Activity devolve-a como resultado. */
    public void setFavorite(boolean isFavorite) { favorite = isFavorite; }

    /**
     * Carrega a meteorologia da hora selecionada em segundo plano.
     * Com retry=false, conserva o pedido/resultado da mesma hora; com true, repete-o.
     * Uma nova seleção invalida o pedido anterior para não mostrar a resposta da hora errada.
     */
    public void load(MarineItem selectedItem, boolean initialFavorite, boolean retry) {
        if (!retry && item != null && item.key().equals(selectedItem.key())) return;
        if (task != null) task.cancel(true);
        int currentRequestVersion = ++requestVersion;
        item = selectedItem;
        detail = null;
        favorite = initialFavorite;
        status.setValue(ExplorerViewModel.Status.LOADING);
        task = executor.submit(() -> {
            try {
                String url = ExplorerApi.detail(selectedItem);
                String response = HttpClient.get(url);
                MarineDetail result = ExplorerParser.detail(response, selectedItem.time);
                if (Thread.currentThread().isInterrupted()) return;
                main.post(() -> {
                    // Pode ter chegado outra seleção ou um cancelamento enquanto a rede respondia.
                    if (currentRequestVersion != requestVersion) return;
                    detail = result;
                    status.setValue(ExplorerViewModel.Status.SUCCESS);
                });
            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) return;
                main.post(() -> {
                    // Pode ter chegado outra seleção ou um cancelamento enquanto a rede respondia.
                    if (currentRequestVersion != requestVersion) return;
                    error = ExplorerError.from(e);
                    status.setValue(ExplorerViewModel.Status.ERROR);
                });
            }
        });
    }

    /** Limpa o painel ao mudar de praia e invalida o pedido anterior. */
    public void clear() {
        requestVersion++;
        if (task != null) task.cancel(true);
        item = null;
        detail = null;
        status.setValue(ExplorerViewModel.Status.IDLE);
    }

    /** Cancela trabalho e impede publicações tardias depois de fechar. */
    @Override protected void onCleared() {
        requestVersion++;
        if (task != null) task.cancel(true);
        executor.shutdownNow();
        main.removeCallbacksAndMessages(null);
    }
}
