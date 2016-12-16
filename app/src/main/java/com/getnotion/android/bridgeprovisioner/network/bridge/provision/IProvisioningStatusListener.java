package com.getnotion.android.bridgeprovisioner.network.bridge.provision;

public interface IProvisioningStatusListener {

    void updateFooterStatus(String status);

    void updateDialogStatus(boolean wasSuccessful, int resultCode);
}
