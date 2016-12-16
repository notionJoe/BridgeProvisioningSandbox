package com.getnotion.android.bridgeprovisioner.models.wrappers;

import com.getnotion.android.bridgeprovisioner.models.NotionBridge;

import java.util.List;

public class BridgeResponse {

    public List<NotionBridge> bridges;

    public BridgeResponse() {
    }

    public BridgeResponse(List<NotionBridge> bridges) {
        this.bridges = bridges;
    }

    public List<NotionBridge> getBridges() {
        return bridges;
    }

    public void setBridges(List<NotionBridge> baseStations) {
        this.bridges = baseStations;
    }
}
