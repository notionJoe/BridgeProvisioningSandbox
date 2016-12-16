package com.getnotion.android.bridgeprovisioner.models;


import io.realm.BuildConfig;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * POJO for Main Application Model -- app specific properties like gcmToken, remaining sensors to install, etc...
 */
public class NotionAppModel extends RealmObject {


    @PrimaryKey
    private String appModelVersion = BuildConfig.VERSION_NAME;
    private String deviceId = ""; // ID of the device this corresponds to on the notion backend
    private String gcmToken = ""; // GCM Token for registering for push notifications
    private int selectedSystemId = -1; // Currently selected systemId

    private boolean loggedIn = false;
    private boolean pushEnabled = true;
    private boolean isBridgeProvisioned = false;
    private boolean setupComplete= false;
    private boolean automaticModeSwitchingEnabled = true;

    public NotionAppModel() {
    }

    public String getAppModelVersion() {
        return appModelVersion;
    }

    public void setAppModelVersion(String appModelVersion) {
        this.appModelVersion = appModelVersion;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getGcmToken() {
        return gcmToken;
    }

    public void setGcmToken(String gcmToken) {
        this.gcmToken = gcmToken;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public boolean isBridgeProvisioned() {
        return isBridgeProvisioned;
    }

    public void setIsBridgeProvisioned(boolean isBridgeProvisioned) {
        this.isBridgeProvisioned = isBridgeProvisioned;
    }

    public boolean isSetupComplete() {
        return setupComplete;
    }

    public void setSetupComplete(boolean setupComplete) {
        this.setupComplete = setupComplete;
    }

    public int getSelectedSystemId() {
        return selectedSystemId;
    }

    public void setSelectedSystemId(int selectedSystemId) {
        this.selectedSystemId = selectedSystemId;
    }

    public boolean isAutomaticModeSwitchingEnabled() {
        return automaticModeSwitchingEnabled;
    }

    public void setAutomaticModeSwitchingEnabled(boolean automaticModeSwitchingEnabled) {
        this.automaticModeSwitchingEnabled = automaticModeSwitchingEnabled;
    }
}
