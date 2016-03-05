package com.sortedunderbelly.pardons;

import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.telephony.PhoneNumberUtils;
import android.widget.EditText;
import android.widget.Toast;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Created by maxr on 3/5/16.
 */
public class ContactPickerHelper {

    public static String handleContactResults(FragmentActivity activity, Intent data,
                                            EditText displayNameEditText) {
        // handle contact results
        Cursor cursor = null;
        String phoneNumber = null;
        try {
            cursor = activity.getContentResolver().query(
                    data.getData(), null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int displayNameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                String displayName = cursor.getString(displayNameIndex);
                displayNameEditText.setText(displayName);
                if (isNullOrEmpty(displayName)) {
                    Toast.makeText(activity, "No display name found for contact.",
                            Toast.LENGTH_SHORT).show();
                }
                int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                phoneNumber = PhoneNumberUtils.normalizeNumber(cursor.getString(phoneIndex));
                if (isNullOrEmpty(phoneNumber)) {
                    Toast.makeText(activity, "No phone number found for contact.",
                            Toast.LENGTH_SHORT).show();
                }
                return phoneNumber;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return phoneNumber;
    }
}
