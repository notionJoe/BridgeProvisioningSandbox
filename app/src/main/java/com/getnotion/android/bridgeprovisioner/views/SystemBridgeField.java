/*
 * Copyright (c) 2015 Loop Labs, Inc. All rights reserved.
 */

package com.getnotion.android.bridgeprovisioner.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import com.getnotion.android.bridgeprovisioner.AppData;
import android.getnotion.android.bridgeprovisioner.R;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.CountDownTimer;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.joanzapata.iconify.Iconify;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.BindString;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * SystemBridgeField -- UI and business logic for searching/selecting a NotionBridge
 *
 * Note! Does not handle permissions -- ensure the user has location permissions enabled before
 * calling {#searchForBridge}
 */
public class SystemBridgeField extends ViewFlipper {

    @Bind(R.id.bridgeTextView)
    TextView bridgeTextView;
    @Bind(R.id.identifyBridgeDescription)
    TextView identifyBridgeDescription;
    @Bind(R.id.systemBridgeFieldBridgeTextView)
    TextView systemBridgeFieldBridgeTextView;
    @Bind(R.id.systemBridgeFieldBridgeIconTextView)
    TextView systemBridgeFieldBridgeIconTextView;
    @Bind(R.id.bridgeNotFoundTextView)
    TextView bridgeNotFoundTextView;
    @Bind(R.id.editIcon)
    TextView editIcon;

    @BindString(R.string.dialogs_wifiNetworksDialog_titleOnlyBridges)
    String stringSelectBridge;
    @BindString(R.string.setup_system_bridge_description_singleBridge)
    String stringSingleBridgeDescription;
    @BindString(R.string.setup_system_bridge_description_multipleBridges)
    String stringMultipleBridgesDescription;

    @BindString(R.string.icons_edit)
    String strIconEdit;
    @BindString(R.string.icons_check_circle)
    String strIconCheckCircle;

    @BindColor(R.color.notion_teal) int notionTeal;
    @BindColor(R.color.notion_mediumish_grey) int notionMediumishGrey;

    private String bridgeHardwareId = "";
    private boolean isSearchingForBridge = false;

    private WifiManager wifiManager;
    private ScanResult bridgeScanResult;
    private List<ScanResult> scanResults = new ArrayList<>();       // All results
    private List<ScanResult> bridgeScanResults = new ArrayList<>(); // Only bridges
    private BroadcastReceiver wifiScanResultsBR;
    private CountDownTimer countDownTimer;


    // Callback for when a bridge is found
    public interface OnBridgeNotFoundListener {
        void onBridgeNotFound();
    }
    private OnBridgeNotFoundListener onBridgeNotFoundListener;


    // Callback for when a bridge is selected
    public interface OnBridgeSelectedListener {
        void onBridgeSelected();
    }
    private OnBridgeSelectedListener onBridgeSelectedListener;

    /**
     * Default Constructor
     *
     * @param context Current context
     * @param attrs AttributeSet
     */
    public SystemBridgeField(Context context, AttributeSet attrs) {
        super(context, attrs);

        setMeasureAllChildren(false);

        // Inflate view/inject view members
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.view_field_system_bridge, this, true);
        ButterKnife.bind(this, view);

        // Reassign error text so that it bolds
        bridgeNotFoundTextView.setText(Html.fromHtml(getContext().getString(R.string.setup_system_bridge_error_bridgeNotFound)));

        // Add icons
        Iconify.addIcons(editIcon);

        // Hide chooserIcon for single bridge
        showButtonChooserIcon(false);

        countDownTimer = new CountDownTimer(10000, 100000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                if (onBridgeNotFoundListener != null) {
                    onBridgeNotFoundListener.onBridgeNotFound();
                }
                showBridgeNotFoundView();
                isSearchingForBridge = false;
            }
        };
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isSearchingForBridge) {
            searchForBridge();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (isSearchingForBridge && wifiScanResultsBR != null) {
            getContext().unregisterReceiver(wifiScanResultsBR);
            wifiScanResultsBR = null;
            countDownTimer.cancel();
        }

    }

    public void setOnBridgeNotFoundListener(OnBridgeNotFoundListener onBridgeNotFoundListener) {
        this.onBridgeNotFoundListener = onBridgeNotFoundListener;
    }

    public void setOnBridgeSelectedListener(OnBridgeSelectedListener onBridgeSelectedListener) {
        this.onBridgeSelectedListener = onBridgeSelectedListener;
    }

    /**
     * Start searching for a NotionBridge access point. If only a single bridge is found, it will
     * be selected and the local onBridgeFound/Selected listeners will be fired
     */
    public void searchForBridge() {

        // Search in progress -- calm down already!
        if(isSearchingForBridge && wifiScanResultsBR != null) {
            return;
        }

        // Grab Wifi manager and turn wifi on if it already isn't
        wifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        // Change to searching for bridge view
        showSearchingForBridgeView();

        // register the BR
        registerWifiScanResultsReceiver();
        isSearchingForBridge = wifiManager.startScan();

        // Start timeout timer
        countDownTimer.start();
    }

    private void registerWifiScanResultsReceiver() {
        if(wifiScanResultsBR != null) return;
        // Register the scan results BroadcastReceiver and then kick off a scan in the background
        wifiScanResultsBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Grab results
                scanResults = wifiManager.getScanResults();

                // No bridge found/selected yet or manually search kicked off...let's see if we can find it in the results
                findBridgesFromScanResults(scanResults);
        }};
        getContext().registerReceiver(wifiScanResultsBR, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private void findBridgesFromScanResults(List<ScanResult> scanResults) {
        // Find all bridge scanResults
        for (ScanResult scanResult : scanResults) {
            if (scanResult.SSID.startsWith(AppData.BRIDGE_PREFIX)) {
                bridgeScanResults.add(scanResult);
            }
        }

        // If there are bridges, select the first, stop searching and show additional UI if there are multiple to select
        if (!bridgeScanResults.isEmpty()) {
            setBridgeScanResult(bridgeScanResults.get(0));
            isSearchingForBridge = false;
            countDownTimer.cancel();
            getContext().unregisterReceiver(wifiScanResultsBR);
            wifiScanResultsBR = null;
            if (onBridgeSelectedListener != null) {
                onBridgeSelectedListener.onBridgeSelected();
            }

            // If there were multiple bridges update the UI to show that they can select another
            showButtonChooserIcon(bridgeScanResults.size() > 1);
        }
    }

    public void collapseField() {
        setDisplayedChild(0);
    }

    public boolean isCollapsed() {
        return getDisplayedChild() == 0;
    }

    public void showBridgeFoundView() {
        setDisplayedChild(1);
    }

    public void showBridgeNotFoundView() {
        setDisplayedChild(2);
    }

    public void showSearchingForBridgeView() {
        setDisplayedChild(3);
    }

    public void editField() {
        if (bridgeScanResult == null) {
            showBridgeNotFoundView();
        } else {
            showBridgeFoundView();
        }
    }

    public void setEditIconVisible(boolean isVisible) {
        View editIcon = findViewById(R.id.editIcon);
        if (editIcon != null) {
            editIcon.setVisibility(isVisible ? VISIBLE : INVISIBLE);
        }
    }

    /**
     * Changes the edit icon to a checkmark to show the bridge as been provisioned
     * @param isProvisioned whether or not the bridge is provisioned
     */
    public void setBridgeProvisioned(boolean isProvisioned) {
        if(isProvisioned) {
            editIcon.setText(strIconCheckCircle);
            editIcon.setTextColor(notionTeal);
        } else {
            editIcon.setText(strIconEdit);
            editIcon.setTextColor(notionMediumishGrey);
        }
    }

    public ScanResult getBridgeScanResult() {
        return bridgeScanResult;
    }

    public void setBridgeScanResult(ScanResult bridgeScanResult) {
        this.bridgeScanResult = bridgeScanResult;
        systemBridgeFieldBridgeTextView.setText(bridgeScanResult.SSID);
        bridgeTextView.setText(bridgeScanResult.SSID);

        // Set the hardware id
        bridgeHardwareId = AppData.BRIDGE_HARDWARE_ID_PREFIX + bridgeScanResult.SSID.substring(AppData.BRIDGE_PREFIX.length());

        showBridgeFoundView();
    }

    public String getBridgeHardwareId() {
        return bridgeHardwareId;
    }

    private void showButtonChooserIcon(boolean isVisible) {
        int visibility = isVisible ? VISIBLE : INVISIBLE;
        systemBridgeFieldBridgeIconTextView.setVisibility(visibility);
        if (isVisible) {
            identifyBridgeDescription.setText(stringMultipleBridgesDescription);
        } else {
            identifyBridgeDescription.setText(stringSingleBridgeDescription);
        }
    }

    /**
     * Show the WifiNetworksDialog when the network field container is clicked
     *
     * @param view view clicked
     */
    @OnClick(R.id.systemBridgeFieldBridgeContainer)
    public void selectNetwork(View view) {
        if (systemBridgeFieldBridgeIconTextView.getVisibility() != VISIBLE) {
            return;
        }

        // Grab ssids
        final List<String> bridgeNames = new ArrayList<>(bridgeScanResults.size());
        for (ScanResult scanResult : bridgeScanResults) {
            bridgeNames.add(scanResult.SSID);
        }

        new AlertDialog.Builder(getContext())
                .setTitle(stringSelectBridge)
                .setItems(bridgeNames.toArray(new CharSequence[bridgeNames.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setBridgeScanResult(bridgeScanResults.get(which));
                    }
                })
                .create()
                .show();
    }
}
