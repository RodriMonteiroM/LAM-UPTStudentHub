package pt.upt.studenthub.models;

import android.content.Intent;

/** Definições imutáveis, guardadas apenas em memória (matéria até à Semana 7). */
public final class ExplorerSettings {
    public final double threshold;
    public final int step, days, refreshSeconds;

    /** Agrupa as quatro escolhas validadas no ecrã de definições. */
    public ExplorerSettings(double threshold, int step, int days, int refreshSeconds) {
        this.threshold = threshold;
        this.step = step;
        this.days = days;
        this.refreshSeconds = refreshSeconds;
    }

    /** Valores usados na primeira abertura. */
    public static ExplorerSettings defaults() { return new ExplorerSettings(1.5, 1, 2, 300); }

    /** Lê as escolhas transportadas pelo Intent, sem usar persistência. */
    public static ExplorerSettings from(Intent intent) {
        return new ExplorerSettings(intent.getDoubleExtra("threshold", 1.5), intent.getIntExtra("step", 1),
                intent.getIntExtra("days", 2), intent.getIntExtra("refresh", 300));
    }

    /** Escreve as escolhas no Intent que vai abrir ou devolver uma Activity. */
    public void put(Intent intent) {
        intent.putExtra("threshold", threshold).putExtra("step", step).putExtra("days", days)
                .putExtra("refresh", refreshSeconds);
    }
}
