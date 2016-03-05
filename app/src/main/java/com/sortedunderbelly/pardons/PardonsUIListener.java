package com.sortedunderbelly.pardons;

/**
 * Events that are triggered by the async storage layer.
 *
 * Created by maxr on 2/26/16.
 */
public interface PardonsUIListener {

    void onRespondToAccusationAgainstMeComplete(Pardons pardons);

    void onMakeAccusationComplete(Accusation accusation);
    void onRetractAccusationComplete(Accusation accusation);

    void onSentPardonsComplete(Pardons pardons);
    void onReceivedPardonsComplete(Pardons pardons);

    void onChangeMyAccusations();
    void onAccusationAgainstMeChangeComplete();

    void onStorageAuthenticationError(String errorStr, String token);
}
