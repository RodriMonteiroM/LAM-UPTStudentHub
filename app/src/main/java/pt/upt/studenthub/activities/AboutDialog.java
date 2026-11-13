package pt.upt.studenthub.activities;

import pt.upt.studenthub.R;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

/** Mostra a caixa de diálogo "About". É usada pelas várias Activities (evita código repetido). */
public final class AboutDialog {

    /** Impede criar instâncias: esta classe reúne apenas métodos estáticos. */
    private AboutDialog() { }

    /** Apresenta o diálogo partilhado com a informação da aplicação. */

    public static void show(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}
