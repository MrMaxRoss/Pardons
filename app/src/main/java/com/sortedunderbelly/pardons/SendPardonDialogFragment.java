package com.sortedunderbelly.pardons;

/**
 * Created by max.ross on 8/15/15.
 */
public class SendPardonDialogFragment extends NewPardonDialogFragment {

    @Override
    protected int getPardonTargetDisplayNameHintResId() {
        return R.string.send_pardon_display_name_hint;
    }

    @Override
    protected int getNewPardonDialogPositiveButtonTextResId() {
        return R.string.sendPardonButtonText;
    }

    @Override
    protected void onNewPardonClick(String phoneNumber, String displayName, int quantity, String reason) {
        getMainActivity().sendPardon(phoneNumber, displayName, quantity, reason);
    }
}
