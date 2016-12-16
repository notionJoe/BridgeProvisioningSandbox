package com.getnotion.android.bridgeprovisioner.models;

import io.realm.RealmObject;

/**
 * RealmInt adapter object for Realm to handle an array of ints -- workaround as there is
 * no support of primitive arrays right now... booo... hiss..... :(
 */
public class RealmString extends RealmObject{

    private String val;

    public RealmString() { }

    public RealmString(String val) {
        this.val = val;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }
}
