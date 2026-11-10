package pt.upt.studenthub.models;

import android.content.Intent;

/** Uma hora de previsão. NaN representa uma medição não disponível, nunca zero inventado. */
public final class MarineItem {
    public final Beach beach;
    public final String time;
    public final double height, direction, period, water;

    /** Cria um registo imutável do pedido A. */
    public MarineItem(Beach beach, String time, double height, double direction, double period, double water) {
        this.beach = beach;
        this.time = time;
        this.height = height;
        this.direction = direction;
        this.period = period;
        this.water = water;
    }

    /** A praia e a hora identificam o favorito mesmo após atualizar a lista. */
    public String key() { return beach.name() + "|" + time; }

    /** Transporta os dados da linha para o detalhe; só são usados tipos simples. */
    public void put(Intent intent) {
        intent.putExtra("beach", beach.name()).putExtra("time", time).putExtra("height", height)
                .putExtra("direction", direction).putExtra("period", period).putExtra("water", water);
    }

    /** Reconstrói a linha recebida por Intent. */
    public static MarineItem from(Intent intent) {
        return new MarineItem(Beach.valueOf(intent.getStringExtra("beach")), intent.getStringExtra("time"),
                intent.getDoubleExtra("height", Double.NaN), intent.getDoubleExtra("direction", Double.NaN),
                intent.getDoubleExtra("period", Double.NaN), intent.getDoubleExtra("water", Double.NaN));
    }
}
