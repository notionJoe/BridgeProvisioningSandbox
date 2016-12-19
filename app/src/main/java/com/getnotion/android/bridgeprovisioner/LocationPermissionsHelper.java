package com.getnotion.android.bridgeprovisioner;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class LocationPermissionsHelper {

    private static final String TAG = LocationPermissionsHelper.class.getSimpleName();
    private static final String[] LOCATIONS_PERMISSIONS = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                                                       Manifest.permission.ACCESS_COARSE_LOCATION};

    public static boolean arePermissionsEnabled(Context context) {
        boolean locationPermissionsEnabled = ContextCompat
                .checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // If on v6.0.0 must check Write Settings permissions -- see MOB-16
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                Build.VERSION.RELEASE.startsWith("6.0") &&
                !Build.VERSION.RELEASE.startsWith("6.0.")) {
            return Settings.System.canWrite(context) && locationPermissionsEnabled;
        }

        return locationPermissionsEnabled;
    }

    /**
     * Check for location permissions on the device
     */
    public static void checkForLocationPermissions(Activity activity, boolean hasCheckedForPermissions) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (!hasCheckedForPermissions) {
                ActivityCompat.requestPermissions(activity,
                                                  LOCATIONS_PERMISSIONS,
                                                  NotionBridgeProvisionerApplication.PERMISSION_REQUEST_FINE_LOCATION);
            }
        } else {
            // TODO: start scanning early?
            //startScanningForBridge();
            Log.d(TAG, "Permissions enabled already");
        }
    }
}
