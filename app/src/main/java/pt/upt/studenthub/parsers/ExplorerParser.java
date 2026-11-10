package pt.upt.studenthub.parsers;

import pt.upt.studenthub.models.Beach;
import pt.upt.studenthub.models.MarineDetail;
import pt.upt.studenthub.models.MarineItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Converte JSON em modelos sem dependências externas, tratando null explicitamente. */
public final class ExplorerParser {
    /** Impede criar instâncias: esta classe reúne apenas métodos estáticos. */
    private ExplorerParser() { }

    /** Lê um número; null significa ausência da medição e texto inesperado é erro de dados. */
    private static double number(JSONObject object, String key) throws JSONException {
        if (!object.has(key)) throw new JSONException("Missing field: " + key);
        return object.isNull(key) ? Double.NaN : object.getDouble(key);
    }

    /** Lê os arrays paralelos e rejeita comprimentos incompatíveis. */
    private static double value(JSONObject object, String key, int index, int count) throws JSONException {
        JSONArray array = object.getJSONArray(key);
        if (array.length() != count) throw new JSONException("Misaligned arrays");
        return array.isNull(index) ? Double.NaN : array.getDouble(index);
    }

    /** Valida a hora antes de a usar para construir o pedido do detalhe. */
    private static String time(String text) throws JSONException {
        if (!text.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) throw new JSONException("Invalid time");
        return text;
    }

    /** Pedido A: seleciona uma linha por hora ou de três em três horas. */
    public static List<MarineItem> list(String body, Beach beach, int step) throws JSONException {
        JSONObject hourly = new JSONObject(body).getJSONObject("hourly");
        JSONArray times = hourly.getJSONArray("time");
        List<MarineItem> items = new ArrayList<>();
        for (int i = 0; i < times.length(); i++) {
            String date = time(times.getString(i));
            double height = value(hourly, "wave_height", i, times.length());
            double direction = value(hourly, "wave_direction", i, times.length());
            double period = value(hourly, "wave_period", i, times.length());
            double water = value(hourly, "sea_surface_temperature", i, times.length());
            if (i % step == 0) items.add(new MarineItem(beach, date, height, direction, period, water));
        }
        return Collections.unmodifiableList(items);
    }

    /** Pedido B: procura a mesma hora da linha; nunca associa pelo índice errado. */
    public static MarineDetail detail(String body, String selectedTime) throws JSONException {
        JSONObject hourly = new JSONObject(body).getJSONObject("hourly");
        JSONArray times = hourly.getJSONArray("time");
        for (int i = 0; i < times.length(); i++) {
            if (selectedTime.equals(times.getString(i))) {
                return new MarineDetail(value(hourly, "wind_speed_10m", i, times.length()),
                        value(hourly, "wind_direction_10m", i, times.length()),
                        value(hourly, "temperature_2m", i, times.length()));
            }
        }
        throw new JSONException("Selected hour missing");
    }

    /** Comparação: lê medições atuais, sem as confundir com as da meia-noite. */
    public static MarineItem current(String body, Beach beach) throws JSONException {
        JSONObject current = new JSONObject(body).getJSONObject("current");
        return new MarineItem(beach, time(current.getString("time")), number(current, "wave_height"),
                number(current, "wave_direction"), number(current, "wave_period"),
                number(current, "sea_surface_temperature"));
    }
}
