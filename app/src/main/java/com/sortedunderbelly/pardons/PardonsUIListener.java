package com.sortedunderbelly.pardons;

/**
 * Created by maxr on 2/26/16.
 */
public interface PardonsUIListener {

    void onApprovePardonsRequest(Pardons pardonsRequest);
    void onDenyPardonsRequest(Pardons pardonsRequest);

    void onAddPardonsRequest(Pardons pardons);
    void onRemovePardonsRequest(Pardons pardons);

    void onAddSentPardons(Pardons pardons);
    void onAddReceivedPardons(Pardons pardons);

    void onChangePendingOutboundPardonsRequests();
    void onChangePendingInboundPardonsRequests();
    void onChangeDeniedOutboundPardonsRequests();
    void onChangeDeniedInboundPardonsRequests();

    void onStorageAuthenticationError(String errorStr, String token);
}
