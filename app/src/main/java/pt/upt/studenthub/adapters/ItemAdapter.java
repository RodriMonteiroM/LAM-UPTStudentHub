package pt.upt.studenthub.adapters;

import pt.upt.studenthub.R;
import pt.upt.studenthub.models.MarineItem;
import pt.upt.studenthub.utils.ExplorerUi;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Adapter personalizado da Semana 3; recicla linhas com o padrão ViewHolder. */
public class ItemAdapter extends ArrayAdapter<MarineItem> {
    private final Set<String> favorites = new HashSet<>();
    private double threshold = 1.5;

    /** Começa com uma lista vazia e usa o layout de linha próprio. */
    public ItemAdapter(Context context) { super(context, R.layout.item_marine); }

    /** Guarda as referências da linha para evitar findViewById em cada atualização. */
    private static final class ViewHolder {
        TextView time, measurements, water;
        ImageView icon, favorite;
        /** Liga as cinco Views uma única vez por linha criada. */
        ViewHolder(View rowView) {
            time = rowView.findViewById(R.id.rowTime);
            measurements = rowView.findViewById(R.id.rowMeasurements);
            water = rowView.findViewById(R.id.rowWater);
            icon = rowView.findViewById(R.id.rowIcon);
            favorite = rowView.findViewById(R.id.rowFavorite);
        }
    }

    /** Substitui os dados após terminar o pedido A. */
    public void submit(List<MarineItem> items) {
        setNotifyOnChange(false);
        clear();
        addAll(items);
        notifyDataSetChanged();
    }

    /** Atualiza apenas a apresentação dos favoritos e do limiar. */
    public void decorate(Set<String> keys, double limit) {
        favorites.clear();
        favorites.addAll(keys);
        threshold = limit;
        notifyDataSetChanged();
    }

    /** Preenche sempre todos os campos, incluindo os ícones de uma linha reciclada. */
    @NonNull @Override public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_marine, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else holder = (ViewHolder) convertView.getTag();
        MarineItem item = getItem(position);
        Context context = getContext();
        holder.time.setText(item.time.replace('T', ' '));
        holder.measurements.setText(context.getString(R.string.ex_row_wave, ExplorerUi.number(context, item.height), ExplorerUi.number(context, item.period)));
        holder.water.setText(context.getString(R.string.ex_row_water, ExplorerUi.direction(context, item.direction), ExplorerUi.number(context, item.water)));
        boolean surf = !Double.isNaN(item.height) && item.height > threshold;
        holder.icon.setImageResource(surf ? R.drawable.ic_surf : R.drawable.ic_wave);
        holder.icon.setContentDescription(context.getString(surf ? R.string.ex_surf : R.string.ex_wave));
        holder.favorite.setVisibility(favorites.contains(item.key()) ? View.VISIBLE : View.INVISIBLE);
        return convertView;
    }
}
