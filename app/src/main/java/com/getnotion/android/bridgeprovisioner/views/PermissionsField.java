/*
 * Copyright (c) 2015 Loop Labs, Inc. All rights reserved.
 */

package com.getnotion.android.bridgeprovisioner.views;

import android.content.Context;
import android.getnotion.android.bridgeprovisioner.R;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;


import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * CollapsibleField for displaying/editing the Permissions
 */
public class PermissionsField extends CollapsibleField {

    private static final String TAG = PermissionsField.class.getSimpleName();

    // Expanded view
    @Bind(R.id.locationPermissionsSwitchCompat)
    SwitchCompat locationPermissionSwitchCompat;
    @Bind(R.id.writeSettingsPermissionsSwitchCompat)
    SwitchCompat writeSettingsPermissionSwitchCompat;
    @Bind(R.id.writeSettingsPermissionsView)
    View writeSettingsPermissionsView;

    private CompoundButton.OnCheckedChangeListener writeSettingsChangeListener;

    public PermissionsField(Context context, AttributeSet attrs) {
        super(context, attrs);

        setMeasureAllChildren(false);

        // Inflate view/inject view members
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.view_field_permissions, this, true);
        ButterKnife.bind(this, view);

        // If on v6.0.0 show the Write Settings permissions -- see MOB-16
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
             Build.VERSION.RELEASE.startsWith("6.0") &&
             !Build.VERSION.RELEASE.startsWith("6.0.")) {
            writeSettingsPermissionsView.setVisibility(VISIBLE);
        }
    }

    /**
     * Set OnCheckedChangeListener for the LocationPermissionsSwitch
     *
     * @param onCheckedChangeListener listener for when permission is toggled
     */
    public void setLocationPermissionOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
        locationPermissionSwitchCompat.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    /**
     * Set OnCheckedChangeListener for the WriteSettingPermissionsSwitch
     *
     * @param onCheckedChangeListener listener for when permission is toggled
     */
    public void setWriteSettingsPermissionsOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
        writeSettingsChangeListener = onCheckedChangeListener;
        writeSettingsPermissionSwitchCompat.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    /**
     * Updates the Write Settings permission switch based off of current system setting
     */
    public void refreshWriteSettingsSwitch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            writeSettingsPermissionSwitchCompat.setOnCheckedChangeListener(null);
            writeSettingsPermissionSwitchCompat.setChecked(Settings.System.canWrite(getContext()));
            writeSettingsPermissionSwitchCompat.setOnCheckedChangeListener(writeSettingsChangeListener);
        }
    }

    /**
     * Returns whether or not all permission toggles have been enabled. Actual underlying permission
     * check is handled when first toggled.
     *
     */
    public boolean getPermissionsEnabled() {
        if (writeSettingsPermissionsView.getVisibility() == VISIBLE) {
            return  writeSettingsPermissionSwitchCompat.isChecked() && locationPermissionSwitchCompat.isChecked();
        } else {
            return locationPermissionSwitchCompat.isChecked();
        }
    }

    @Override
    public boolean isValid() {
        return getPermissionsEnabled();
    }

    @Override
    protected void focusFirstField() {
        // Not needed
    }

    @Override
    protected void setDetailsViewValues() {
        // Not needed
    }
}
