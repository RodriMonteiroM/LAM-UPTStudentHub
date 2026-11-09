package pt.upt.studenthub;

/**
 * Modelo imutável com os dados que aparecem no ecrã.
 * Todos os campos são final: depois de criado não muda, por isso pode passar
 * da thread de trabalho para a thread principal sem problemas de concorrência.
 */
public final class WeatherReport {

    public final double temperature; // °C
    public final int humidity;       // 0-100 %
    public final double windSpeed;   // km/h
    public final int weatherCode;    // código WMO
    public final String time;        // hora local ISO, ex.: "2026-10-15T10:45"

    public WeatherReport(double temperature, int humidity, double windSpeed,
                         int weatherCode, String time) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.weatherCode = weatherCode;
        this.time = time;
    }

    /** Fica só com a parte das horas: "2026-10-15T10:45" -> "10:45". */
    public String hour() {
        int separator = time.indexOf('T');
        return separator >= 0 ? time.substring(separator + 1) : time;
    }
}
