package pt.upt.studenthub;

import pt.upt.studenthub.models.Beach;
import pt.upt.studenthub.models.ExplorerError;
import pt.upt.studenthub.models.ExplorerSettings;
import pt.upt.studenthub.models.MarineDetail;
import pt.upt.studenthub.models.MarineItem;
import pt.upt.studenthub.network.ExplorerApi;
import pt.upt.studenthub.network.HttpClient;
import pt.upt.studenthub.parsers.ExplorerParser;


import android.content.Intent;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import pt.upt.studenthub.utils.ExplorerUi;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.util.List;
import static org.junit.Assert.*;

/** Testes com JSON local: não dependem da disponibilidade do serviço Open-Meteo. */
@RunWith(AndroidJUnit4.class)
public class ExplorerDataTest {
    /** Duas horas com arrays alinhados; a segunda altura é intencionalmente null. */
    private String sample() {
        return "{\"hourly\":{\"time\":[\"2026-11-11T00:00\",\"2026-11-11T01:00\"],"
                + "\"wave_height\":[1.2,null],\"wave_direction\":[270,280],"
                + "\"wave_period\":[9,10],\"sea_surface_temperature\":[17,18]}}";
    }
    /** Confirma a leitura dos dados e a associação correta à praia. */
    @Test public void parsesAlignedArrays() throws Exception {
        List<MarineItem> items = ExplorerParser.list(sample(), Beach.MATOSINHOS, 1);
        assertEquals(2, items.size()); assertEquals(1.2, items.get(0).height, 0.001);
        assertEquals(Beach.MATOSINHOS, items.get(0).beach);
    }
    /** Null nunca é convertido para zero. */
    @Test public void handlesNullMeasurement() throws Exception {
        assertTrue(Double.isNaN(ExplorerParser.list(sample(), Beach.NAZARE, 1).get(1).height));
    }
    /** O filtro de três horas salta os índices intermédios. */
    @Test public void filtersThreeHourStep() throws Exception {
        assertEquals(1, ExplorerParser.list(sample(), Beach.NAZARE, 3).size());
    }
    /** Arrays vazios válidos representam resultado vazio. */
    @Test public void acceptsEmptyResult() throws Exception {
        String json = "{\"hourly\":{\"time\":[],\"wave_height\":[],\"wave_direction\":[],\"wave_period\":[],\"sea_surface_temperature\":[]}}";
        assertTrue(ExplorerParser.list(json, Beach.POVOA, 1).isEmpty());
    }
    /** Uma resposta incompleta deve originar mensagem de dados inválidos. */
    @Test(expected = JSONException.class) public void rejectsMissingArrays() throws Exception {
        ExplorerParser.list("{\"hourly\":{\"time\":[\"2026-11-11T00:00\"]}}", Beach.POVOA, 1);
    }
    /** Não aceita índices desalinhados entre medições e horas. */
    @Test(expected = JSONException.class) public void rejectsMisalignedArrays() throws Exception {
        ExplorerParser.list(sample().replace("[9,10]", "[9]"), Beach.POVOA, 1);
    }
    /** O pedido B deve escolher a hora exata e não apenas o primeiro elemento. */
    @Test public void matchesDetailHour() throws Exception {
        String json = "{\"hourly\":{\"time\":[\"2026-11-11T00:00\",\"2026-11-11T01:00\"],"
                + "\"wind_speed_10m\":[10,20],\"wind_direction_10m\":[90,180],\"temperature_2m\":[15,null]}}";
        MarineDetail detail = ExplorerParser.detail(json, "2026-11-11T01:00");
        assertEquals(20, detail.wind, 0.001); assertTrue(Double.isNaN(detail.air));
    }
    /** Uma hora inexistente não pode mostrar valores de outra hora. */
    @Test(expected = JSONException.class) public void rejectsMissingDetailHour() throws Exception {
        ExplorerParser.detail("{\"hourly\":{\"time\":[]}}", "2026-11-11T01:00");
    }
    /** A comparação usa current e trata medições atuais nulas. */
    @Test public void parsesCurrentConditions() throws Exception {
        String json = "{\"current\":{\"time\":\"2026-11-11T10:15\",\"wave_height\":null,"
                + "\"wave_direction\":270,\"wave_period\":9,\"sea_surface_temperature\":17}}";
        MarineItem item = ExplorerParser.current(json, Beach.NAZARE);
        assertTrue(Double.isNaN(item.height)); assertEquals("2026-11-11T10:15", item.time);
    }
    /** Valida os parâmetros sem depender da ordem nem da codificação textual do URL. */
    @Test public void buildsVariantUrls() {
        Uri url = Uri.parse(ExplorerApi.list(Beach.POVOA, new ExplorerSettings(1.5, 3, 1, 60)));
        assertEquals("https", url.getScheme()); assertEquals("1", url.getQueryParameter("forecast_days"));
        assertEquals("Europe/Lisbon", url.getQueryParameter("timezone"));
        assertTrue(url.getQueryParameter("hourly").contains("sea_surface_temperature"));
    }
    /** Confirma a troca de definições por Intent. */
    @Test public void settingsRoundTrip() {
        Intent intent = new Intent(); new ExplorerSettings(2.5, 3, 1, 60).put(intent);
        ExplorerSettings s = ExplorerSettings.from(intent);
        assertEquals(2.5, s.threshold, 0.001); assertEquals(3, s.step); assertEquals(60, s.refreshSeconds);
    }
    /** Distingue os quatro erros que a interface tem de explicar. */
    @Test public void mapsErrorsPrecisely() {
        assertEquals(ExplorerError.Kind.TIMEOUT, ExplorerError.from(new SocketTimeoutException()).kind);
        assertEquals(ExplorerError.Kind.NO_CONNECTION, ExplorerError.from(new IOException()).kind);
        assertEquals(503, ExplorerError.from(new HttpClient.HttpStatusException(503)).code);
        assertEquals(ExplorerError.Kind.PARSE, ExplorerError.from(new JSONException("invalid")).kind);
    }
    /** A direção apresenta o ponto cardeal, trata 360 graus e medições em falta. */
    @Test public void formatsCardinalDirections() {
        Context context = ApplicationProvider.getApplicationContext();
        assertTrue(ExplorerUi.direction(context, 270).startsWith("O ("));
        assertTrue(ExplorerUi.direction(context, 360).startsWith("N ("));
        assertEquals(context.getString(R.string.ex_missing), ExplorerUi.direction(context, Double.NaN));
    }
}
