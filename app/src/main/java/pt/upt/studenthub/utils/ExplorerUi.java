package pt.upt.studenthub.utils;

import pt.upt.studenthub.R;
import pt.upt.studenthub.models.ExplorerError;
import pt.upt.studenthub.models.MarineDetail;
import pt.upt.studenthub.models.MarineItem;
import pt.upt.studenthub.viewmodels.DetailViewModel;
import pt.upt.studenthub.viewmodels.ExplorerViewModel;

import android.content.Context;
import android.widget.TextView;
import java.util.Locale;

/** Formatação partilhada pelos ecrãs; os textos visíveis vêm dos recursos XML. */
public final class ExplorerUi {
    /** Impede criar instâncias: esta classe reúne apenas métodos estáticos. */
    private ExplorerUi() { }

    /** Um valor em falta é apresentado como traço, nunca como zero. */
    public static String number(Context context, double value) {
        return Double.isNaN(value) ? context.getString(R.string.ex_missing) : String.format(Locale.getDefault(), "%.1f", value);
    }

    /** Converte graus para um dos oito pontos cardeais e conserva o valor numérico. */
    public static String direction(Context context, double degrees) {
        if (!Double.isFinite(degrees)) return context.getString(R.string.ex_missing);
        double normalized = (degrees % 360 + 360) % 360;
        int index = (int) Math.round(normalized / 45) % 8;
        String cardinal = context.getResources().getStringArray(R.array.ex_cardinal_points)[index];
        return context.getString(R.string.ex_direction, cardinal, number(context, degrees));
    }

    /** Converte o erro técnico numa mensagem que o utilizador compreende. */
    public static String error(Context context, ExplorerError error) {
        if (error == null) return context.getString(R.string.ex_invalid);
        switch (error.kind) {
            case HTTP: return context.getString(R.string.ex_http, error.code);
            case TIMEOUT: return context.getString(R.string.ex_timeout);
            case NO_CONNECTION: return context.getString(R.string.ex_no_connection);
            default: return context.getString(R.string.ex_invalid);
        }
    }

    /** Mostra a informação da linha sem fazer pedidos de rede. */
    public static String marine(Context context, MarineItem item) {
        return context.getString(R.string.ex_marine, context.getString(item.beach.nameRes), item.time.replace('T', ' '),
                number(context, item.height), number(context, item.period), direction(context, item.direction), number(context, item.water));
    }

    /** Preenche o cartão, combinando os dados do mar com os do segundo endpoint. */
    public static void detail(TextView view, DetailViewModel viewModel) {
        Context context = view.getContext();
        if (viewModel.getItem() == null) {
            view.setText(R.string.ex_select);
            return;
        }
        String text = marine(context, viewModel.getItem());
        if (viewModel.getStatus().getValue() == ExplorerViewModel.Status.LOADING) text += "\n\n" + context.getString(R.string.ex_loading);
        else if (viewModel.getStatus().getValue() == ExplorerViewModel.Status.ERROR) text += "\n\n" + error(context, viewModel.getError());
        else if (viewModel.getDetail() != null) {
            MarineDetail detail = viewModel.getDetail();
            text += "\n\n" + context.getString(R.string.ex_air, number(context, detail.air), number(context, detail.wind), direction(context, detail.direction));
        }
        view.setText(text);
    }
}
