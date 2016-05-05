package com.sortedunderbelly.pardons;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import static android.provider.ContactsContract.Contacts.CONTENT_URI;
import static com.google.common.base.Strings.isNullOrEmpty;


/**
 * Fragment that displays the dialog where the user sends a new pardon.
 * <p/>
 * Created by max.ross on 8/8/15.
 */
public class NewPardonDialogFragment extends DialogFragment {

    private static final int CONTACT_PICKER_RESULT = 1001;

    String pardonTargetEmail;
    EditText pardonTargetDisplayName;
    EditText pardonQuantityText;
    EditText pardonReasonText;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.create_pardon, null);
        pardonTargetEmail = null;
        pardonTargetDisplayName = (EditText) view.findViewById(R.id.pardon_target_display_name);
        pardonTargetDisplayName.setHint(R.string.send_pardon_display_name_hint);
        pardonQuantityText = (EditText) view.findViewById(R.id.pardonQuantityText);
        pardonReasonText = (EditText) view.findViewById(R.id.pardonReasonText);

        Button contactSelectorButton = (Button) view.findViewById(R.id.do_target_picker);
        builder.setView(view)
                .setPositiveButton(R.string.sendPardonButtonText, new DialogInterface.OnClickListener() {
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

        contactSelectorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, CONTENT_URI);
                startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
            }
        });
        return builder.create();
    }

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
                    // contact chooser so we are guaranteed to get the email number
                    // behind the scenes.
                    if (!isNullOrEmpty(pardonTargetEmail) &&
                            hasText(pardonTargetDisplayName) &&
                            hasText(pardonReasonText) &&
                            hasText(pardonQuantityText) &&
                            Integer.parseInt(pardonQuantityText.getText().toString()) > 0) {
                        dismiss();
                        getMainActivity().sendPardons(
                                pardonTargetEmail,
                                pardonTargetDisplayName.getText().toString(),
                                Integer.parseInt(pardonQuantityText.getText().toString()),
                                pardonReasonText.getText().toString());
                    } else {
                        // display message asking user to provide target, reason, and quantity
                        Utils.simpleErrorDialog(getActivity(),
                                getResources(), R.string.missingPardonDataMessage);
                        // dialog stays open
                    }
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case CONTACT_PICKER_RESULT:
                    pardonTargetEmail =
                            ContactPickerHelper.handleContactResults(getActivity(), data, pardonTargetDisplayName);
                    break;
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
