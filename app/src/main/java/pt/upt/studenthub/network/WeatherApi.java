package pt.upt.studenthub.network;

import android.net.Uri;

/**
 * Único sítio onde se constroem os URLs da Open-Meteo (HTTPS, grátis, sem chave).
 * Usamos Uri.Builder em vez de juntar Strings à mão porque ele codifica
 * corretamente vírgulas, barras, espaços, etc.
 */
public final class WeatherApi {

    // Coordenadas da Universidade Portucalense (Porto)
    public static final double UPT_LATITUDE = 41.18;
    public static final double UPT_LONGITUDE = -8.61;

    private static final String CURRENT_FIELDS =
            "temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code";

    /** Impede criar instâncias: esta classe reúne apenas métodos estáticos. */
    private WeatherApi() { }

    /** URL normal: .../v1/forecast?latitude=..&longitude=..&current=..&timezone=.. */
    public static String currentWeatherUrl() {
        return build("forecast");
    }

    /** Mesmo pedido mas para um caminho que não existe -> o servidor responde com erro HTTP (404). */
    public static String brokenUrl() {
        return build("forecast-does-not-exist");
    }

    /** Monta o URL completo; só muda o último segmento do caminho. */
    private static String build(String path) {
        return new Uri.Builder()
                .scheme("https")
                .authority("api.open-meteo.com")
                .appendPath("v1")
                .appendPath(path)
                .appendQueryParameter("latitude", String.valueOf(UPT_LATITUDE))
                .appendQueryParameter("longitude", String.valueOf(UPT_LONGITUDE))
                .appendQueryParameter("current", CURRENT_FIELDS)
                .appendQueryParameter("timezone", "Europe/Lisbon")
                .build()
                .toString();
    }
}
