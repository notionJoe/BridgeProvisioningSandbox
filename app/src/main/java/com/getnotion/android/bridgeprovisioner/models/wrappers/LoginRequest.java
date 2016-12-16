/*
 * Copyright (c) 2015 Loop Labs, Inc. All rights reserved.
 */

package com.getnotion.android.bridgeprovisioner.models.wrappers;

/**
 * LoginRequest wrapper for the "/api/users/sign_in" endpoint
 */
public class LoginRequest {

    private LoginSession sessions;

    public LoginRequest() {
    }

    public LoginRequest(LoginSession session) {
        this.sessions = session;
    }

    public LoginSession getSessions() {
        return sessions;
    }

    public void setSessions(LoginSession session) {
        this.sessions = session;
    }

    public static class LoginSession {
        private String email;
        private String password;

        public LoginSession(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }
}

