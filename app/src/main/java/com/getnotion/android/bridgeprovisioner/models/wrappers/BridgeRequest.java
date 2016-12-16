package com.getnotion.android.bridgeprovisioner.models.wrappers;

import com.getnotion.android.bridgeprovisioner.models.NotionBridge;

public class BridgeRequest {

    public NotionBridge bridges;

    public BridgeRequest() {
    }

    public BridgeRequest(NotionBridge baseStation) {
        this.bridges = baseStation;
    }

    public NotionBridge getBridge() {
        return bridges;
    }

    public void setBridge(NotionBridge baseStation) {
        this.bridges = baseStation;
    }

}
