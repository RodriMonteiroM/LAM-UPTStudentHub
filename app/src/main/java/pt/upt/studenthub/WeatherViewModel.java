package pt.upt.studenthub;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Mesma arquitetura da Semana 6 (Executor + Handler + LiveData), mas agora o trabalho
 * demorado é um pedido HTTP verdadeiro. Sobrevive à rotação, por isso o pedido não se repete.
 * Não guarda Activity/View/Context: os erros saem como enum e a Activity escolhe o texto.
 */
public class WeatherViewModel extends ViewModel {

    public enum Status { IDLE, LOADING, SUCCESS, ERROR }

    public enum ErrorType { NO_CONNECTION, TIMEOUT, HTTP, PARSE }

    // Uma só thread de trabalho: os pedidos ficam em fila, nunca em paralelo
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    // Ponte para voltar à thread principal
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<Status> status = new MutableLiveData<>(Status.IDLE);
    private final MutableLiveData<WeatherReport> report = new MutableLiveData<>();
    private final MutableLiveData<ErrorType> error = new MutableLiveData<>();

    private int lastHttpCode; // só é alterado na thread principal
    private Future<?> currentTask;

    // Para fora só sai LiveData (só de leitura)
    public LiveData<Status> getStatus() { return status; }
    public LiveData<WeatherReport> getReport() { return report; }
    public LiveData<ErrorType> getError() { return error; }
    public int getLastHttpCode() { return lastHttpCode; }

    /**
     * Chamado na thread principal. Põe logo o estado em LOADING e manda a transferência e a
     * conversão do JSON para a thread de trabalho. O resultado (ou o erro) volta à thread
     * principal através do Handler e é publicado com setValue().
     */
    public void load(String url) {
        if (status.getValue() == Status.LOADING) {
            return; // já há um pedido a decorrer: não se faz outro
        }
        status.setValue(Status.LOADING);

        currentTask = executor.submit(() -> {
            // ---- daqui para baixo: THREAD DE TRABALHO ----
            try {
                String body = HttpClient.get(url);                  // rede (bloqueia)
                WeatherReport result = WeatherParser.parse(body);   // JSON -> objeto
                mainHandler.post(() -> {                            // ---- THREAD PRINCIPAL ----
                    report.setValue(result);
                    status.setValue(Status.SUCCESS);
                });
            // As subclasses de IOException têm de vir primeiro, senão não compila
            } catch (HttpClient.HttpStatusException e) {
                postError(ErrorType.HTTP, e.code);          // servidor respondeu 4xx/5xx
            } catch (SocketTimeoutException e) {
                postError(ErrorType.TIMEOUT, 0);            // servidor demorou demasiado
            } catch (IOException e) {
                postError(ErrorType.NO_CONNECTION, 0);      // sem rede, DNS falhou, ligação recusada...
            } catch (JSONException e) {
                postError(ErrorType.PARSE, 0);              // resposta num formato inesperado
            }
        });
    }

    /** Publica o erro na thread principal. Os últimos dados válidos NÃO são apagados (continuam visíveis). */
    private void postError(ErrorType type, int httpCode) {
        mainHandler.post(() -> {
            lastHttpCode = httpCode;
            error.setValue(type);
            status.setValue(Status.ERROR);
        });
    }

    /** Só é chamado quando o ecrã fecha de vez (não na rotação): cancela e limpa tudo. */
    @Override
    protected void onCleared() {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
