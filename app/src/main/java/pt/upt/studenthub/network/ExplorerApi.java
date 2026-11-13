package pt.upt.studenthub.network;

import pt.upt.studenthub.models.Beach;
import pt.upt.studenthub.models.ExplorerSettings;
import pt.upt.studenthub.models.MarineItem;

import android.net.Uri;

/** Único sítio do módulo 3 onde são construídos URLs, sempre com Uri.Builder. */
public final class ExplorerApi {
    /** Impede criar instâncias: esta classe reúne apenas métodos estáticos. */
    private ExplorerApi() { }

    /** Parte comum: HTTPS, coordenadas e fuso horário pedido no enunciado. */
    private static Uri.Builder base(String host, String path, Beach beach) {
        return new Uri.Builder().scheme("https").authority(host).path(path)
                .appendQueryParameter("latitude", String.valueOf(beach.latitude))
                .appendQueryParameter("longitude", String.valueOf(beach.longitude))
                .appendQueryParameter("timezone", "Europe/Lisbon");
    }

    /** Pedido A: séries horárias da ondulação e temperatura da água. */
    public static String list(Beach beach, ExplorerSettings settings) {
        return base("marine-api.open-meteo.com", "/v1/marine", beach)
                .appendQueryParameter("hourly", "wave_height,wave_direction,wave_period,sea_surface_temperature")
                .appendQueryParameter("forecast_days", String.valueOf(settings.days)).build().toString();
    }

    /** Pedido B: meteorologia do dia selecionado; o parser escolhe a hora exata. */
    public static String detail(MarineItem item) {
        String date = item.time.substring(0, 10);
        return base("api.open-meteo.com", "/v1/forecast", item.beach)
                .appendQueryParameter("hourly", "wind_speed_10m,wind_direction_10m,temperature_2m")
                .appendQueryParameter("start_date", date).appendQueryParameter("end_date", date)
                .build().toString();
    }

    /** Um destes pedidos é lançado em cada uma das três threads da comparação. */
    public static String compare(Beach beach) {
        return base("marine-api.open-meteo.com", "/v1/marine", beach)
                .appendQueryParameter("current", "wave_height,wave_direction,wave_period,sea_surface_temperature")
                .build().toString();
    }
}
