package com.getnotion.android.bridgeprovisioner.network;

public enum NotionEnvironment {

    STAGING("https://api.staging.getnotion.com"),
    PRODUCTION("https://api.getnotion.com");

    NotionEnvironment(String endpoint) {
        mEndpoint = endpoint;
    }

    private String mEndpoint;

    public String getEndpoint() {
        return mEndpoint;
    }
}
