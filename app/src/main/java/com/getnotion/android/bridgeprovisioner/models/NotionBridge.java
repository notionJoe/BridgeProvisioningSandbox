/*
 * Copyright (c) 2015 Loop Labs, Inc. All rights reserved.
 */

package com.getnotion.android.bridgeprovisioner.models;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * POJO for a System object to be persisted locally/retrieved via API
 */
public class NotionBridge extends RealmObject {

    @PrimaryKey
    private int id;
    private String name;
    private String mode;
    private String hardwareId;
    private String firmwareVersion;
    private Date missingAt;
    private Date createdAt;
    private Date updatedAt;
    private int systemId; // For POSTing
    private NotionBridgeLinks links;


    public NotionBridge() {
    }

    public NotionBridge(String hardwareId, int systemId) {
        this.hardwareId = hardwareId;
        this.systemId = systemId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHardwareId() {
        return hardwareId;
    }

    public void setHardwareId(String hardwareId) {
        this.hardwareId = hardwareId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public int getSystemId() {
        return systemId;
    }

    public void setSystemId(int systemId) {
        this.systemId = systemId;
    }

    public Date getCreatedAt() { return createdAt; }

    public void setCreatedAt(Date set) {
        createdAt = set;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getMissingAt() {
        return missingAt;
    }

    public void setMissingAt(Date missingAt) {
        this.missingAt = missingAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public NotionBridgeLinks getLinks() {
        return links;
    }

    public void setLinks(NotionBridgeLinks links) {
        this.links = links;
    }
}
