/*
 * Copyright (c) 2015 Loop Labs, Inc. All rights reserved.
 */

package com.getnotion.android.bridgeprovisioner.models;

import java.util.Date;
import java.util.List;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * POJO for a System object to be persisted locally/retrieved via API
 */
public class NotionSystem extends RealmObject {

    @PrimaryKey
    private int id;
    private String name;
    private String mode;
    private double latitude;
    private double longitude;
    private String timezoneId;          // When GETing
    private String deviceTimezoneId;    // When POSTing
    private String locality;
    private String administrativeArea;
    private String postalCode;
    private String fireNumber;
    private String policeNumber;
    private String emergencyNumber;
    private Date nightTimeStart;
    private Date nightTimeEnd;
    private Date createdAt;
    private Date updatedAt;
    private NotionSystemLinks links;

    // System Modes
    @Ignore public static String HOME = "home";
    @Ignore public static String AWAY = "away";
    @Ignore public static String NIGHT_TIME = "night_time";
    @Ignore public static String DO_NOT_DISTRUB = "do_not_disturb";

    public NotionSystem() {
    }

    public NotionSystem(String name, String deviceTimezoneId, double latitude, double longitude) {
        this.name = name;
        this.deviceTimezoneId = deviceTimezoneId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getAdministrativeArea() {
        return administrativeArea;
    }

    public void setAdministrativeArea(String administrativeArea) {
        this.administrativeArea = administrativeArea;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getFireNumber() {
        return fireNumber;
    }

    public void setFireNumber(String fireNumber) {
        this.fireNumber = fireNumber;
    }

    public String getPoliceNumber() {
        return policeNumber;
    }

    public void setPoliceNumber(String policeNumber) {
        this.policeNumber = policeNumber;
    }

    public String getEmergencyNumber() {
        return emergencyNumber;
    }

    public void setEmergencyNumber(String emergencyNumber) {
        this.emergencyNumber = emergencyNumber;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getDeviceTimezoneId() {
        return deviceTimezoneId;
    }

    public void setDeviceTimezoneId(String deviceTimezoneId) {
        this.deviceTimezoneId = deviceTimezoneId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getTimezoneId() {
        return timezoneId;
    }

    public void setTimezoneId(String timezoneId) {
        this.timezoneId = timezoneId;
    }

    public Date getNightTimeStart() {
        return nightTimeStart;
    }

    public void setNightTimeStart(Date nightTimeStart) {
        this.nightTimeStart = nightTimeStart;
    }

    public Date getNightTimeEnd() {
        return nightTimeEnd;
    }

    public void setNightTimeEnd(Date nightTimeEnd) {
        this.nightTimeEnd = nightTimeEnd;
    }

    public NotionSystemLinks getLinks() {
        return links;
    }

    public void setLinks(NotionSystemLinks links) {
        this.links = links;
    }

    /**
     * SystemRequest wrapper for the NotionSystem POJO
     */
    public static class SystemRequest {

        public NotionSystem systems;

        public SystemRequest() {
        }

        public SystemRequest(NotionSystem system) {
            this.systems = system;
        }

        public NotionSystem getSystem() {
            return systems;
        }

        public void setSystem(NotionSystem system) {
            this.systems = system;
        }
    }

    /**
     * SystemRequest wrapper for PUTing just the location of a NotionSystem
     */
    public static class SystemLocationRequest {

        public NotionSystemLocation systems;

        public SystemLocationRequest() {
        }

        public SystemLocationRequest(NotionSystem systems) {
            this.systems = new NotionSystemLocation(String.valueOf(systems.getLatitude()), String.valueOf(systems.getLongitude()));
        }

        public NotionSystemLocation getSystems() {
            return systems;
        }

        public void setSystems(NotionSystemLocation systems) {
            this.systems = new NotionSystemLocation(String.valueOf(systems.getLatitude()), String.valueOf(systems.getLongitude()));
        }
    }

    public static class NotionSystemLocation {
        public String latitude;
        public String longitude;

        public NotionSystemLocation(String latitude, String longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getLatitude() {
            return latitude;
        }

        public void setLatitude(String latitude) {
            this.latitude = latitude;
        }

        public String getLongitude() {
            return longitude;
        }

        public void setLongitude(String longitude) {
            this.longitude = longitude;
        }
    }

    /**
     * SystemResponse wrapper for the NotionSystem POJO
     */
    public static class SystemResponse {

        public List<NotionSystem> systems;

        public SystemResponse() {
        }

        public SystemResponse(List<NotionSystem> systems) {
            this.systems = systems;
        }

        public List<NotionSystem> getSystems() {
            return systems;
        }

        public void setSystems(List<NotionSystem> systems) {
            this.systems = systems;
        }
    }

    /**
     * SystemPOSTResponse wrapper for the NotionSystem POJO
     * <p/>
     * This is necessary as the POST response returns a systems obj, and not array
     */
    public static class SystemPOSTResponse {

        public NotionSystem systems;

        public SystemPOSTResponse() {
        }

        public SystemPOSTResponse(NotionSystem systems) {
            this.systems = systems;
        }

        public NotionSystem getSystems() {
            return systems;
        }

        public void setSystems(NotionSystem systems) {
            this.systems = systems;
        }
    }

    /**
     * SystemResponse wrapper for PUTing a NotionSystem
     */
    public static class SystemPUTResponse {

        public NotionSystem systems;

        public SystemPUTResponse() {
        }

        public SystemPUTResponse(NotionSystem systems) {
            this.systems = systems;
        }

        public NotionSystem getSystems() {
            return systems;
        }

        public void setSystems(NotionSystem systems) {
            this.systems = systems;
        }
    }

    /**
     * SystemResponse wrapper for GETing a NotionSystem
     */
    public static class SystemGETResponse {

        public NotionSystem systems;

        public SystemGETResponse() {
        }

        public SystemGETResponse(NotionSystem systems) {
            this.systems = systems;
        }

        public NotionSystem getSystems() {
            return systems;
        }

        public void setSystems(NotionSystem systems) {
            this.systems = systems;
        }
    }
}