package com.sortedunderbelly.pardons;

/**
 * Created by max.ross on 8/15/15.
 */
public class RequestPardonDialogFragment extends NewPardonDialogFragment {

    @Override
    protected int getPardonTargetDisplayNameHintResId() {
        return R.string.request_pardon_display_name_hint;
    }

    @Override
    protected int getNewPardonDialogPositiveButtonTextResId() {
        return R.string.requestPardonButtonText;
    }

    @Override
    protected void onNewPardonClick(String phoneNumber, String displayName, int quantity, String reason) {
        getMainActivity().requestPardon(phoneNumber, displayName, quantity, reason);
    }
}
