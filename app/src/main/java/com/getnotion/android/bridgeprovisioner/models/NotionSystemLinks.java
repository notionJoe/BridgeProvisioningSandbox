package com.getnotion.android.bridgeprovisioner.models;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * NotionSystem Links obj
 */
public class NotionSystemLinks extends RealmObject {

    private RealmList<RealmInt> sensors;
    private RealmList<RealmInt> locations;

    public NotionSystemLinks() {
    }

    public RealmList<RealmInt> getSensors() {
        return sensors;
    }

    public void setSensors(RealmList<RealmInt> sensors) {
        this.sensors = sensors;
    }

    public RealmList<RealmInt> getLocations() {
        return locations;
    }

    public void setLocations(RealmList<RealmInt> locations) {
        this.locations = locations;
    }
}