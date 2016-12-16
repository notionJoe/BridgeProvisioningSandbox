package com.getnotion.android.bridgeprovisioner.network.bridge.provision;


public interface IBridgeProvisioner {

    void provision(BridgeConfig bridgeConfig, ProvisioningResult provisioningResult) throws Exception;
}
