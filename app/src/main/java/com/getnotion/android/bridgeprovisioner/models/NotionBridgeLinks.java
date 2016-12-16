package com.getnotion.android.bridgeprovisioner.models;

import io.realm.RealmObject;

public class NotionBridgeLinks extends RealmObject {

    private int user;

    public NotionBridgeLinks () {
    }

    public int getUser() {
        return user;
    }

    public void setUser(int user) {
        this.user = user;
    }
}
