package com.sortedunderbelly.pardons;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Resources;

/**
 * Created by max.ross on 8/16/15.
 */
public class Utils {
    public static void simpleErrorDialog(Activity activity, String message) {
        new AlertDialog.Builder(activity)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public static void simpleErrorDialog(Activity activity, Resources resources, int messageId) {
        simpleErrorDialog(activity, resources.getString(messageId));
    }

    public static String getPossiblyPluralPardonString(Resources res, Pardons pardons) {
        return pardons.getQuantity() == 1 ?
                res.getString(R.string.singular_pardon_string) :
                res.getString(R.string.plural_pardon_string);
    }
}
