package com.getnotion.android.bridgeprovisioner;

import android.app.Application;
import android.getnotion.android.bridgeprovisioner.R;
import android.os.Build;
import android.util.Log;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class NotionBridgeProvisionerApplication extends Application{

    private static final String TAG = "NotionBridgeProvisioner";

    // App level constants
    public static final int PERMISSION_REQUEST_FINE_LOCATION = 0;
    public static final int PERMISSION_REQUEST_COARSE_LOCATION = 2;
    public static final int PERMISSION_REQUEST_CAMERA = 3;

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                                                  .setDefaultFontPath(getString(R.string.font_regular))
                                                  .setFontAttrId(R.attr.fontPath)
                                                  .build());
        } catch (Exception e) {
            Log.e(TAG, "Error loading custom font -- using system default.");
            Log.e(TAG, "Version: " + Build.VERSION.RELEASE);
            e.printStackTrace();
        }

    }
}
