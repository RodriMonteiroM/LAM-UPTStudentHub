package pt.upt.studenthub.models;

import pt.upt.studenthub.network.HttpClient;

import java.io.IOException;
import java.net.SocketTimeoutException;

/** Erro sem Context nem texto da interface; a Activity escolhe a string correspondente. */
public final class ExplorerError {
    public enum Kind { NO_CONNECTION, TIMEOUT, HTTP, PARSE }
    public final Kind kind;
    public final int code;

    /** Guarda o tipo e, quando existe, o código HTTP. */
    public ExplorerError(Kind kind, int code) {
        this.kind = kind;
        this.code = code;
    }

    /** A ordem distingue timeout e HTTP antes da IOException mais geral. */
    public static ExplorerError from(Exception e) {
        if (e instanceof HttpClient.HttpStatusException)
            return new ExplorerError(Kind.HTTP, ((HttpClient.HttpStatusException)e).code);
        if (e instanceof SocketTimeoutException) return new ExplorerError(Kind.TIMEOUT, 0);
        if (e instanceof IOException) return new ExplorerError(Kind.NO_CONNECTION, 0);
        return new ExplorerError(Kind.PARSE, 0);
    }
}
