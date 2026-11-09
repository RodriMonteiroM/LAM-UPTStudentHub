package pt.upt.studenthub;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

/** Mostra a caixa de diálogo "About". É usada pelas várias Activities (evita código repetido). */
public final class AboutDialog {

    private AboutDialog() { }

    public static void show(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}
