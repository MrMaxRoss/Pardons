package com.sortedunderbelly.pardons;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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
        Uri result = data.getData();
        String id = result.getLastPathSegment();
        String emailAddress = null;
        Cursor cursor = null;
        try {
            cursor = activity.getContentResolver().query(result, null, null, null, null);
            String displayName = null;
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            }
            if (isNullOrEmpty(displayName)) {
                Toast.makeText(activity, "No display name found for contact.",
                        Toast.LENGTH_SHORT).show();
            } else {
                displayNameEditText.setText(displayName);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        try {
            cursor = activity.getContentResolver().query(
                    Email.CONTENT_URI, null, Email.CONTACT_ID + " = ? ", new String[] {id}, null);
            if (cursor != null && cursor.moveToFirst()) {
                emailAddress = cursor.getString(cursor.getColumnIndex(Email.DATA));
                // TODO(max.ross) Handle multiple email addresses
                if (isNullOrEmpty(emailAddress)) {
                    Toast.makeText(activity, "No email address found for contact.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return emailAddress;
    }
}
