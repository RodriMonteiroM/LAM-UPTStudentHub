package pt.upt.studenthub.utils;

import pt.upt.studenthub.R;

import androidx.annotation.StringRes;

/**
 * Traduz o código WMO enviado pelo servidor para um recurso de texto.
 * Devolve o ID do recurso (R.string...) e não a String, por isso não precisa de Context
 * e os textos continuam todos nos ficheiros XML.
 */
public final class WeatherCodes {

    /** Impede criar instâncias: esta classe reúne apenas métodos estáticos. */
    private WeatherCodes() { }

    /** Devolve o recurso de texto correspondente ao código WMO, ou a descrição desconhecida. */
    @StringRes
    public static int describe(int code) {
        if (code == 0) return R.string.wmo_clear;
        if (code == 1) return R.string.wmo_mainly_clear;
        if (code == 2) return R.string.wmo_partly_cloudy;
        if (code == 3) return R.string.wmo_overcast;
        if (code == 45 || code == 48) return R.string.wmo_fog;
        if (code >= 51 && code <= 57) return R.string.wmo_drizzle;
        if (code >= 61 && code <= 67) return R.string.wmo_rain;
        if (code >= 71 && code <= 77) return R.string.wmo_snow;
        if (code >= 80 && code <= 82) return R.string.wmo_showers;
        if (code == 85 || code == 86) return R.string.wmo_snow_showers;
        if (code >= 95 && code <= 99) return R.string.wmo_thunderstorm;
        return R.string.wmo_unknown;
    }
}
