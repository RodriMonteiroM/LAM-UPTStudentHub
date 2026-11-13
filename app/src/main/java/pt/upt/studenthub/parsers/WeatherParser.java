package pt.upt.studenthub.parsers;

import pt.upt.studenthub.models.WeatherReport;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Converte o texto JSON do servidor num WeatherReport (org.json).
 *
 * Formato esperado (resumido):
 * { "current": { "time": "2026-10-15T10:45", "temperature_2m": 17.3,
 *                "relative_humidity_2m": 80, "wind_speed_10m": 9.4, "weather_code": 3 } }
 */
public final class WeatherParser {

    /** Impede criar instâncias: esta classe reúne apenas métodos estáticos. */
    private WeatherParser() { }

    /**
     * Lança JSONException se faltar alguma chave ou o tipo estiver errado;
     * o ViewModel apanha-a e mostra o erro "dados inválidos".
     */
    public static WeatherReport parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);               // documento inteiro
        JSONObject current = root.getJSONObject("current");  // objeto lá dentro com os valores atuais

        return new WeatherReport(
                current.getDouble("temperature_2m"),
                current.getInt("relative_humidity_2m"),
                current.getDouble("wind_speed_10m"),
                current.getInt("weather_code"),
                current.getString("time"));
    }
}
