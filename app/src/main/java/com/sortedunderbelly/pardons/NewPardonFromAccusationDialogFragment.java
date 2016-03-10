package com.sortedunderbelly.pardons;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


/**Introdu
 * Fragment that displays the dialog where the user sends a pardon that is created from an
 * accusation.
 * <p/>
 */
public class NewPardonFromAccusationDialogFragment extends DialogFragment {

    public static final String ACCUSATION = "accusation";

    Accusation accusation;
    TextView accuserName;
    TextView accuserReason;
    EditText pardonQuantityText;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        accusation = (Accusation) getArguments().getSerializable(NewPardonFromAccusationDialogFragment.ACCUSATION);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.create_pardon_from_accusation, null);
        accuserName = (TextView) view.findViewById(R.id.pardonFromAccusationTargetNameTextView);
        accuserName.setText(accusation.getAccuserDisplay());
        accuserReason = (TextView) view.findViewById(R.id.pardonFromAccusationReasonValueTextView);
        accuserReason.setText(accusation.getReason());
        pardonQuantityText = (EditText) view.findViewById(R.id.pardonFromAccusationQuantityValueTextView);

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
                    if (hasText(pardonQuantityText) &&
                        Integer.parseInt(pardonQuantityText.getText().toString()) > 0) {
                        dismiss();
                        getMainActivity().respondToAccusation(accusation,
                                Integer.parseInt(pardonQuantityText.getText().toString()));
                    } else {
                        // display message asking user to provide quantity
                        Utils.simpleErrorDialog(getActivity(),
                                getResources(), R.string.missingPardonFromAccusationDataMessage);
                        // dialog stays open
                    }
                }
            });
        }
    }

    protected MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    private static boolean hasText(EditText text) {
        return text.getText().length() > 0;
    }

}
