package com.getnotion.android.bridgeprovisioner;

import android.app.ActivityManager;
import android.content.Context;
import android.support.test.espresso.*;

public class ProvisioningIdleResource implements IdlingResource {

    private AddNewBridgeActivity addNewBridgeActivity;
    private ResourceCallback callback;

    public ProvisioningIdleResource(AddNewBridgeActivity addNewBridgeActivity) {
        this.addNewBridgeActivity = addNewBridgeActivity;
    }

    @Override
    public String getName() {
        return "ProvisioningIdleResource";
    }

    @Override
    public boolean isIdleNow() {
        boolean idle = isIdle();

        if (idle) {
            callback.onTransitionToIdle();
            return true;
        }

        return false;
    }

    private boolean isIdle() {
        CharSequence footerText = addNewBridgeActivity.footerButton.getText();
        System.out.println("footer text = " + footerText);
        if (!addNewBridgeActivity.networkField.isSearchingForNetwork()
                && !footerText.toString().equals("Connecting to Bridge...")
                && !footerText.toString().equals("Configuring Bridge...")
                && !footerText.toString().equals("Bridge is connecting...")
                && !footerText.toString().equals("Disconnecting from Bridge...")
                && !footerText.toString().equals("Disconnected from Bridge!")) {
            System.out.println("isIdle = true");
            return true;
        }
        System.out.println("isIdle = false");
        return false;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        this.callback = resourceCallback;

    }
}
