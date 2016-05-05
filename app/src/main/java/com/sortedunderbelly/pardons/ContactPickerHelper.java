package com.sortedunderbelly.pardons;

import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.support.v4.app.FragmentActivity;
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
        try {
            cursor = activity.getContentResolver().query(
                    data.getData(), null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String emailAddress = null;
                int displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                String displayName = cursor.getString(displayNameIndex);
                displayNameEditText.setText(displayName);
                if (isNullOrEmpty(displayName)) {
                    Toast.makeText(activity, "No display name found for contact.",
                            Toast.LENGTH_SHORT).show();
                }
                int emailAddressIndex = cursor.getColumnIndex(Email.DATA);
                emailAddress = cursor.getString(emailAddressIndex);
                // TODO(max.ross) Handle multiple email addresses
                if (isNullOrEmpty(emailAddress)) {
                    Toast.makeText(activity, "No email address found for contact.",
                            Toast.LENGTH_SHORT).show();
                }
                return emailAddress;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }
}
