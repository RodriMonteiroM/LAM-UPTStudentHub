package pt.upt.studenthub.models;

import pt.upt.studenthub.R;

/** Locais fixos da variante 3; as coordenadas são as do enunciado. */
public enum Beach {
    MATOSINHOS(41.18, -8.72, R.string.beach_matosinhos),
    POVOA(41.38, -8.78, R.string.beach_povoa),
    NAZARE(39.60, -9.08, R.string.beach_nazare);
    public final double latitude, longitude;
    public final int nameRes;

    /** Guarda as coordenadas e o recurso com o nome da praia. */
    Beach(double latitude, double longitude, int nameRes) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.nameRes = nameRes;
    }
}
