package com.getnotion.android.bridgeprovisioner.network.bridge.provision;

// Interface for communication with parent activity
public interface OnBridgeProvisionedCallback {
    void onBridgeProvisioned(boolean successful, boolean authFailure, String message);
}
