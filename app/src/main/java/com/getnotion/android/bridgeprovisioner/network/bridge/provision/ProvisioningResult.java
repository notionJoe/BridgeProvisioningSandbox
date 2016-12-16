package com.getnotion.android.bridgeprovisioner.network.bridge.provision;

public class ProvisioningResult {

    private static final String BRIDGE_HAS_NOT_BEEN_PROVISIONED = "Bridge has not been provisioned";

    public static final String NOT_CONFIGURED = "Not configured.";
    public static final String BRIDGE_PROVISIONED = "Bridge provisioned!";
    public static final String AUTH_FAILURE = "Authentication failed. Double-check your network credentials";
    public static final String NETWORK_NOT_FOUND = "Network not found.";
    public static final String INSECURE_PROVISIONING_SUCCESS = "Insecure Provisioning Completed Successfully.";
    public static final String INSECURE_PROVISIONING_FAILED = "Insecure Provisioning Failed.";
    public static final String SECURE_PROVISIONING_SUCCESS = "Bridge Provisioning completed successfully";
    public static final String SECURE_PROVISIONING_FAILED = "Secure Provisioning failed";
    public static final String DHCP_FAILED = "DHCP failed. Please try again";
    public static final String UNKNOWN_ERROR = "Unknown error. Please try again";

    private boolean networkNotFound;
    private boolean completed;
    private String message;
    private boolean isAuthFailure;


    public ProvisioningResult() {
        this.completed = false;
        this.isAuthFailure = false;
        this.networkNotFound = false;
        this.message = BRIDGE_HAS_NOT_BEEN_PROVISIONED;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public boolean isAuthFailure() {
        return isAuthFailure;
    }

    public void setAuthFailure(boolean authFailure) {
        this.isAuthFailure = authFailure;
    }

    public boolean isNetworkNotFound() {
        return networkNotFound;
    }

    public void setNetworkNotFound(boolean networkNotFound) {
        this.networkNotFound = networkNotFound;
    }

    public void setResult(boolean completed, boolean isAuthFailure, boolean networkNotFound, String message) {
        setCompleted(completed);
        setAuthFailure(isAuthFailure);
        setNetworkNotFound(networkNotFound);
        setMessage(message);
    }
}
