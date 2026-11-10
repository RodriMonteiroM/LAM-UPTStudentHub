package pt.upt.studenthub.viewmodels;

import pt.upt.studenthub.models.Beach;
import pt.upt.studenthub.models.ExplorerError;
import pt.upt.studenthub.models.MarineItem;
import pt.upt.studenthub.network.ExplorerApi;
import pt.upt.studenthub.network.HttpClient;
import pt.upt.studenthub.parsers.ExplorerParser;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Três threads, uma por praia; uma falha não cancela as restantes. */
public class CompareViewModel extends ViewModel {

    /** Uma coluna da comparação: resultado, erro ou ainda em carregamento. */
    public static final class Result {
        public final Beach beach;
        public final MarineItem item;
        public final ExplorerError error;
        /** Um resultado imutável por local. */
        public Result(Beach beach, MarineItem item, ExplorerError error) {
            this.beach = beach;
            this.item = item;
            this.error = error;
        }
    }
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<List<Result>> results = new MutableLiveData<>(Collections.emptyList());
    private final List<Result> slots = new ArrayList<>();
    private boolean started, cleared;

    /** Cada publicação contém os três locais, na mesma ordem. */
    public LiveData<List<Result>> getResults() { return results; }

    /** Chamado uma vez; a rotação observa os mesmos resultados sem repetir pedidos. */
    public void load() {
        if (started) return;
        started = true;
        for (Beach beach : Beach.values()) slots.add(new Result(beach, null, null));
        publish();
        for (Beach beach : Beach.values()) {
            executor.submit(() -> {
                Result result;
                try {
                    String url = ExplorerApi.compare(beach);
                    String response = HttpClient.get(url);
                    MarineItem item = ExplorerParser.current(response, beach);
                    result = new Result(beach, item, null);
                } catch (Exception e) {
                    result = new Result(beach, null, ExplorerError.from(e));
                }
                if (Thread.currentThread().isInterrupted()) return;
                final Result completed = result;
                main.post(() -> {
                    if (cleared) return;
                    slots.set(beach.ordinal(), completed);
                    publish();
                });
            });
        }
    }

    /** Só a main thread altera slots; não é necessário um bloqueio entre threads. */
    private void publish() { results.setValue(Collections.unmodifiableList(new ArrayList<>(slots))); }

    /** Interrompe todos os pedidos ao abandonar o ecrã. */
    @Override protected void onCleared() {
        cleared = true;
        executor.shutdownNow();
        main.removeCallbacksAndMessages(null);
    }
}
