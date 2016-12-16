package com.getnotion.android.bridgeprovisioner.models;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * NotionUser Links obj
 */
public class NotionUserLinks extends RealmObject {

    private RealmList<RealmInt> systems;
    private RealmList<RealmInt> devices;
    private int secondaryContact;

    public NotionUserLinks() {
    }

    public RealmList<RealmInt> getSystems() {
        return systems;
    }

    public void setSystems(RealmList<RealmInt> systems) {
        this.systems = systems;
    }

    public RealmList<RealmInt> getDevices() {
        return devices;
    }

    public void setDevices(RealmList<RealmInt> devices) {
        this.devices = devices;
    }

    public int getSecondaryContact() {
        return secondaryContact;
    }

    public void setSecondaryContact(int secondaryContact) {
        this.secondaryContact = secondaryContact;
    }
}