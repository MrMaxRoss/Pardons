package com.sortedunderbelly.pardons;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.telephony.PhoneNumberUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import static com.google.common.base.Strings.isNullOrEmpty;


/**
 * Fragment that displays the dialog where the user sends a new pardon.
 * <p/>
 * Created by max.ross on 8/8/15.
 */
public abstract class NewPardonDialogFragment extends DialogFragment {

    private static final int CONTACT_PICKER_RESULT = 1001;

    String pardonTargetPhoneNumber;
    EditText pardonTargetDisplayName;
    EditText pardonQuantityText;
    EditText pardonReasonText;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.send_pardon, null);
        pardonTargetPhoneNumber = null;
        pardonTargetDisplayName = (EditText) view.findViewById(R.id.pardon_target_display_name);
        pardonTargetDisplayName.setHint(getPardonTargetDisplayNameHintResId());
        pardonQuantityText = (EditText) view.findViewById(R.id.pardonQuantityText);
        pardonReasonText = (EditText) view.findViewById(R.id.pardonReasonText);

        Button contactSelectorButton = (Button) view.findViewById(R.id.do_target_picker);
        contactSelectorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Phone.CONTENT_URI);
                startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
            }
        });
        builder.setView(view)
                .setPositiveButton(getNewPardonDialogPositiveButtonTextResId(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing here because we override to change the close behavior.
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // nothing to do
                    }
                });
        return builder.create();
    }

    protected abstract int getNewPardonDialogPositiveButtonTextResId();

    protected abstract int getPardonTargetDisplayNameHintResId();

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog alertDialog = (AlertDialog) getDialog();
        if (alertDialog != null) {
            Button positiveButton = alertDialog.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // create if all fields are populated
                    // TODO(max.ross) Make the display name field only editable via the
                    // contact chooser so we are guaranteed to get the phone number
                    // behind the scenes.
                    if (!isNullOrEmpty(pardonTargetPhoneNumber) &&
                            hasText(pardonTargetDisplayName) &&
                            hasText(pardonReasonText) &&
                            hasText(pardonQuantityText) &&
                            Integer.parseInt(pardonQuantityText.getText().toString()) > 0) {
                        dismiss();
                        onNewPardonClick(
                                pardonTargetPhoneNumber,
                                pardonTargetDisplayName.getText().toString(),
                                Integer.parseInt(pardonQuantityText.getText().toString()),
                                pardonReasonText.getText().toString());
                    } else {
                        // display message asking user to provide target and reason
                        Utils.simpleErrorDialog(getActivity(),
                                getResources(), R.string.missingMessage);
                        // dialog stays open
                    }
                }
            });
        }
    }

    protected abstract void onNewPardonClick(
            String phoneNumber, String displayName, int quantity, String reason);

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case CONTACT_PICKER_RESULT:
                    // handle contact results
                    Cursor cursor = null;
                    try {
                        cursor = getActivity().getContentResolver().query(
                                data.getData(), null, null, null, null);
                        if (cursor.moveToFirst()) {
                            int displayNameIndex = cursor.getColumnIndex(Phone.DISPLAY_NAME);
                            String displayName = cursor.getString(displayNameIndex);
                            pardonTargetDisplayName.setText(displayName);
                            if (isNullOrEmpty(displayName)) {
                                Toast.makeText(getActivity(), "No display name found for contact.",
                                        Toast.LENGTH_SHORT).show();
                            }
                            int phoneIndex = cursor.getColumnIndex(Phone.NUMBER);
                            pardonTargetPhoneNumber =
                                    PhoneNumberUtils.normalizeNumber(cursor.getString(phoneIndex));
                            if (isNullOrEmpty(pardonTargetPhoneNumber)) {
                                Toast.makeText(getActivity(), "No phone number found for contact.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        break;
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
            }
        }
        // TODO(max.ross) handle failure case
    }

    protected MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    private static boolean hasText(EditText text) {
        return text.getText().length() > 0;
    }

}
