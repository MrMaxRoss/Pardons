package com.sortedunderbelly.pardons;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import static com.google.common.base.Strings.isNullOrEmpty;


/**Introdu
 * Fragment that displays the dialog where the user sends a new pardon.
 * <p/>
 * Created by max.ross on 8/8/15.
 */
public class NewPardonDialogFragment extends DialogFragment {

    private static final int CONTACT_PICKER_RESULT = 1001;

    public static final String ACCUSATION = "accusation";

    String pardonTargetPhoneNumber;
    EditText pardonTargetDisplayName;
    EditText pardonQuantityText;
    EditText pardonReasonText;
    @Nullable
    Accusation accusation;

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
        accusation = (Accusation) getArguments().getSerializable(NewPardonDialogFragment.ACCUSATION);
        if (accusation != null) {
            pardonTargetDisplayName.setText(accusation.getAccuser());
            // can't edit the target display name if it was provided
            pardonTargetDisplayName.setEnabled(false);
            pardonTargetPhoneNumber = "blar!";
            pardonReasonText.setText(accusation.getReason());
            // can't edit the reason if an accusation was provided
            pardonReasonText.setEnabled(false);

            // hide the contact picker button if an accusation was provided
            contactSelectorButton.setVisibility(View.GONE);
        } else {
            contactSelectorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, Phone.CONTENT_URI);
                    startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
                }
            });
        }
        return builder.create();
    }

    protected int getPardonTargetDisplayNameHintResId() {
        return R.string.send_pardon_display_name_hint;
    }

    protected int getNewPardonDialogPositiveButtonTextResId() {
        return R.string.sendPardonButtonText;
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
                    if (!isNullOrEmpty(pardonTargetPhoneNumber) &&
                            hasText(pardonTargetDisplayName) &&
                            hasText(pardonReasonText) &&
                            hasText(pardonQuantityText) &&
                            Integer.parseInt(pardonQuantityText.getText().toString()) > 0) {
                        dismiss();
                        if (accusation == null) {
                            getMainActivity().sendPardons(
                                    pardonTargetPhoneNumber,
                                    pardonTargetDisplayName.getText().toString(),
                                    Integer.parseInt(pardonQuantityText.getText().toString()),
                                    pardonReasonText.getText().toString());
                        } else {
                            getMainActivity().respondToAccusation(accusation,
                                    Integer.parseInt(pardonQuantityText.getText().toString()));
                        }
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case CONTACT_PICKER_RESULT:
                    pardonTargetPhoneNumber =
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
