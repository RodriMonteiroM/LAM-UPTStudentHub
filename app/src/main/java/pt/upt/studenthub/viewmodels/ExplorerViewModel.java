package pt.upt.studenthub.viewmodels;

import pt.upt.studenthub.models.Beach;
import pt.upt.studenthub.models.ExplorerError;
import pt.upt.studenthub.models.ExplorerSettings;
import pt.upt.studenthub.models.MarineItem;
import pt.upt.studenthub.network.ExplorerApi;
import pt.upt.studenthub.network.HttpClient;
import pt.upt.studenthub.parsers.ExplorerParser;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Estado do Explorer: sobrevive à rotação e não guarda referências à interface. */
public class ExplorerViewModel extends ViewModel {
    public enum Status { IDLE, LOADING, SUCCESS, EMPTY, ERROR, CANCELLED }
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<Status> status = new MutableLiveData<>(Status.IDLE);
    private final MutableLiveData<List<MarineItem>> items = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Set<String>> favorites = new MutableLiveData<>(Collections.emptySet());
    private final MutableLiveData<ExplorerSettings> settings = new MutableLiveData<>(ExplorerSettings.defaults());
    private final Set<String> favoriteKeys = new HashSet<>();
    private Future<?> task;
    // Cada pedido tem uma versão; cancelar invalida respostas antigas ainda na fila.
    private int requestVersion;
    private Beach beach = Beach.MATOSINHOS;
    private ExplorerError error;
    private MarineItem selected;
    private boolean batteryLow;

    /** Estado observável, sem permissão de escrita para a Activity. */
    public LiveData<Status> getStatus() { return status; }

    /** Última lista válida; mantém-se quando há erro ou cancelamento. */
    public LiveData<List<MarineItem>> getItems() { return items; }

    /** Favoritos observáveis, identificados pela praia e hora. */
    public LiveData<Set<String>> getFavorites() { return favorites; }

    /** Escolhas observáveis; ficam em memória. */
    public LiveData<ExplorerSettings> getSettings() { return settings; }

    /** Praia atualmente escolhida. */
    public Beach getBeach() { return beach; }

    /** Último erro, convertido em texto pela Activity. */
    public ExplorerError getError() { return error; }

    /** Linha selecionada para o painel de paisagem. */
    public MarineItem getSelected() { return selected; }

    /** Guarda a seleção sem depender da View. */
    public void select(MarineItem item) { selected = item; }

    /** Estado da bateria conservado durante a rotação. */
    public boolean isBatteryLow() { return batteryLow; }

    /** Atualiza a informação recebida pelo BroadcastReceiver. */
    public void setBatteryLow(boolean low) { batteryLow = low; }

    /** Verifica se uma determinada linha é favorita. */
    public boolean isFavorite(String key) { return favoriteKeys.contains(key); }

    /** Publica uma cópia para que a interface nunca altere o conjunto interno. */
    public void setFavorite(String key, boolean favorite) {
        if (favorite) {
            favoriteKeys.add(key);
        } else {
            favoriteKeys.remove(key);
        }
        favorites.setValue(Collections.unmodifiableSet(new HashSet<>(favoriteKeys)));
    }

    /** Aplica definições e recarrega; não é chamado durante um pedido. */
    public void apply(ExplorerSettings value) {
        settings.setValue(value);
        load();
    }

    /** Muda a praia e inicia o pedido A. */
    public void changeBeach(Beach value) {
        beach = value;
        selected = null;
        load();
    }

    /** Rede e JSON em fundo; o Handler entrega o resultado à main thread. */
    public void load() {
        if (status.getValue() == Status.LOADING) return;
        final int currentRequestVersion = ++requestVersion;
        final Beach selectedBeach = beach;
        final ExplorerSettings settingsSnapshot = settings.getValue();
        status.setValue(Status.LOADING);
        task = executor.submit(() -> {
            try {
                String url = ExplorerApi.list(selectedBeach, settingsSnapshot);
                String response = HttpClient.get(url);
                List<MarineItem> result = ExplorerParser.list(response, selectedBeach, settingsSnapshot.step);
                if (Thread.currentThread().isInterrupted()) return;
                main.post(() -> {
                    if (currentRequestVersion != requestVersion) return; // ignora respostas de pedidos já cancelados
                    items.setValue(result);
                    status.setValue(result.isEmpty() ? Status.EMPTY : Status.SUCCESS);
                });
            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) return;
                main.post(() -> {
                    if (currentRequestVersion != requestVersion) return;
                    error = ExplorerError.from(e);
                    status.setValue(Status.ERROR);
                });
            }
        });
    }

    /** Invalida também resultados que já estavam na fila do Handler. */
    public void cancel() {
        if (status.getValue() != Status.LOADING) return;
        requestVersion++;
        if (task != null) task.cancel(true);
        status.setValue(Status.CANCELLED);
    }

    /** Fecha os recursos quando o ecrã termina definitivamente, nunca na rotação. */
    @Override protected void onCleared() {
        requestVersion++;
        if (task != null) task.cancel(true);
        executor.shutdownNow();
        main.removeCallbacksAndMessages(null);
    }
}
