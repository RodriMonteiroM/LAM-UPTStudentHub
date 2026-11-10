package pt.upt.studenthub.models;

/** Medições da mesma hora, obtidas pelo segundo endpoint (pedido B). */
public final class MarineDetail {
    public final double wind, direction, air;

    /** Guarda vento, direção e temperatura do ar. */
    public MarineDetail(double wind, double direction, double air) {
        this.wind = wind;
        this.direction = direction;
        this.air = air;
    }
}
