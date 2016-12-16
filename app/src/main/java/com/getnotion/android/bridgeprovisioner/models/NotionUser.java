/*
 * Copyright (c) 2015 Loop Labs, Inc. All rights reserved.
 */

package com.getnotion.android.bridgeprovisioner.models;



import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * POJO for a User object to be persisted locally/retrieved via API
 */
public class NotionUser extends RealmObject {

    @PrimaryKey
    private int id;
    private String firstName;
    private String lastName;
    private String authenticationToken;
    private String email;
    private String phoneNumber;
    private String role;
    private String organization;
    private Date createdAt;
    private Date updatedAt;
    private NotionUserLinks links;

    @Ignore
    private String password;
    @Ignore
    private String passwordConfirmation;

    public NotionUser() {
    }

    public NotionUser(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.passwordConfirmation = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAuthenticationToken() {
        return authenticationToken;
    }

    public void setAuthenticationToken(String authenticationToken) {
        this.authenticationToken = authenticationToken;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirmation() {
        return passwordConfirmation;
    }

    public void setPasswordConfirmation(String passwordConfirmation) {
        this.passwordConfirmation = passwordConfirmation;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
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

    public NotionUserLinks getLinks() {
        return links;
    }

    public void setLinks(NotionUserLinks links) {
        this.links = links;
    }

    /**
     * UserRequest wrapper for the NotionUser POJO
     */
    public static class UserRequest {

        public NotionUser users;

        public UserRequest() {
        }

        public UserRequest(NotionUser user) {
            this.users = user;
        }

        public NotionUser getUser() {
            return users;
        }

        public void setUser(NotionUser user) {
            this.users = user;
        }
    }

    /**
     * UserResponse for the NotionUser POJO
     */
    public static class UserResponse {

        public NotionUser users;

        public UserResponse() {
        }

        public UserResponse(NotionUser user) {
            this.users = user;
        }

        public NotionUser getUser() {
            return users;
        }

        public void setUser(NotionUser user) {
            this.users = user;
        }
    }
}
