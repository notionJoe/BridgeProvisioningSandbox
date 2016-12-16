package com.getnotion.android.bridgeprovisioner.models.wrappers;

import com.getnotion.android.bridgeprovisioner.models.NotionBridge;

public class BridgePostResponse {
    public NotionBridge baseStations;

    public BridgePostResponse() {
    }

    public BridgePostResponse(NotionBridge baseStation) {
        this.baseStations = baseStation;
    }

    public NotionBridge getBridge() {
        return baseStations;
    }

    public void setBridge(NotionBridge baseStation) {
        this.baseStations = baseStation;
    }
}
