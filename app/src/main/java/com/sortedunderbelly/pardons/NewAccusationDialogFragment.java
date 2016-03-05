package com.sortedunderbelly.pardons;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import static com.google.common.base.Strings.isNullOrEmpty;


/**
 * Fragment that displays the dialog where the user sends a new pardon.
 * <p/>
 * Created by max.ross on 8/8/15.
 */
public class NewAccusationDialogFragment extends DialogFragment {

    private static final int CONTACT_PICKER_RESULT = 1001;

    String accusationTargetPhoneNumber;
    EditText accusationTargetDisplayName;
    EditText accusationReasonText;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.make_accusation, null);
        accusationTargetPhoneNumber = null;
        accusationTargetDisplayName = (EditText) view.findViewById(R.id.accusation_target_display_name);
        accusationTargetDisplayName.setHint(getPardonTargetDisplayNameHintResId());
        accusationReasonText = (EditText) view.findViewById(R.id.accusationReasonText);

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
        // TODO(max.ross): Add pardon icon
        // builder.setIcon(android.R.drawable.ic_dialog_alert);
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
                    // contact chooser so we are guaranteed to get the phone number
                    // behind the scenes.
                    if (!isNullOrEmpty(accusationTargetPhoneNumber) &&
                            hasText(accusationTargetDisplayName) &&
                            hasText(accusationReasonText)) {
                        dismiss();
                        onNewAccusationClick(
                                accusationTargetPhoneNumber,
                                accusationTargetDisplayName.getText().toString(),
                                accusationReasonText.getText().toString());
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

    protected void onNewAccusationClick(
            String phoneNumber, String displayName, String reason) {
        getMainActivity().makeAccusation(phoneNumber, displayName, reason);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case CONTACT_PICKER_RESULT:
                    accusationTargetPhoneNumber =
                            ContactPickerHelper.handleContactResults(getActivity(), data, accusationTargetDisplayName);
                        break;
            }
        }
        // TODO(max.ross) handle failure case
    }

    protected int getPardonTargetDisplayNameHintResId() {
        return R.string.make_accusation_display_name_hint;
    }

    protected int getNewPardonDialogPositiveButtonTextResId() {
        return R.string.accuseButtonText;
    }

    protected MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    private static boolean hasText(EditText text) {
        return text.getText().length() > 0;
    }

}
