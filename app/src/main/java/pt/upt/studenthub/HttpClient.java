package pt.upt.studenthub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Cliente HTTP simples com HttpURLConnection (já vem no Android, sem bibliotecas).
 * É código BLOQUEANTE: só pode ser chamado a partir de uma thread de fundo.
 */
public final class HttpClient {

    /** Tempo máximo para ligar e para esperar por dados (ms). */
    static final int TIMEOUT_MS = 10_000;

    private HttpClient() { }

    /**
     * O servidor respondeu, mas com um código diferente de 200 (ex.: 404, 500).
     * Estende IOException para poder ser lançada pelo get(), mas guarda o código para o mostrar.
     */
    public static final class HttpStatusException extends IOException {

        public final int code;

        public HttpStatusException(int code) {
            super("HTTP " + code);
            this.code = code;
        }
    }

    /**
     * Faz um GET e devolve o corpo da resposta como texto.
     * Ordem: abrir -> configurar -> getResponseCode() (é aqui que o pedido é enviado)
     * -> verificar o código -> ler o corpo em UTF-8 -> disconnect() no finally.
     */
    public static String get(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);                     // limite para abrir a ligação
            conn.setReadTimeout(TIMEOUT_MS);                        // limite à espera de dados
            conn.setRequestProperty("Accept", "application/json");  // cabeçalho: queremos JSON

            // Envia o pedido e bloqueia até chegar o código de estado
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new HttpStatusException(code); // não vale a pena ler o corpo
            }

            // try-with-resources fecha o reader (e a stream) mesmo que haja exceção
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Se o ViewModel cancelou a tarefa (cancel(true)), paramos a leitura
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedIOException("Request cancelled");
                    }
                    body.append(line);
                }
            }
            return body.toString();
        } finally {
            conn.disconnect(); // liberta sempre a ligação
        }
    }
}
