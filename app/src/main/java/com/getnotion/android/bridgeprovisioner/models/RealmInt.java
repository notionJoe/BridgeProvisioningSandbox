package com.getnotion.android.bridgeprovisioner.models;

import io.realm.RealmObject;

/**
 * RealmInt adapter object for Realm to handle an array of ints -- workaround as there is
 * no support of primitive arrays right now... booo... hiss..... :(
 */
public class RealmInt extends RealmObject{

    private int val;

    public RealmInt() { }

    public RealmInt(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }

    public void setVal(int val) {
        this.val = val;
    }
}
