/*
 * Copyright (c) 2015 Loop Labs, Inc. All rights reserved.
 */

package com.getnotion.android.bridgeprovisioner.views;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import com.getnotion.android.bridgeprovisioner.NetworkUtils;

import android.getnotion.android.bridgeprovisioner.R;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
 *
 */
public class SystemNetworkField extends ViewFlipper {

    private static final String TAG = SystemNetworkField.class.getSimpleName();

    private final Context context;

    // Over/collapsed view
    @Bind(R.id.systemNetworkFieldNetworkTextView)
    TextView networkTextView;
    @Bind(R.id.systemNetworkFieldPasswordTextView)
    TextView passwordTextView;

    // Details/expanded view
    @Bind(R.id.systemNetworkFieldPasswordLabelTextView)
    TextView systemNetworkFieldPasswordLabelTextView;
    @Bind(R.id.systemNetworkFieldNetworkContainer)
    View systemNetworkFieldNetworkContainer;
    @Bind(R.id.systemNetworkFieldNetworkChooserTextView)
    TextView networkChooserTextView;
    @Bind(R.id.systemNetworkFieldPasswordContainer)
    View passwordContainer;
    @Bind(R.id.systemNetworkFieldPasswordEditText)
    EditText passwordEditText;
    @Bind(R.id.systemNetworkFieldPasswordError)
    TextView passwordErrorTextView;

    @Bind(R.id.editIcon)
    TextView editIcon;
    @Bind(R.id.eyeIcon)
    TextView eyeIcon;

    @BindColor(R.color.notion_dark_teal)
    int notionDarkTeal;
    @BindColor(R.color.notion_medium_grey)
    int notionMediumGrey;

    @BindString(R.string.setup_system_network_selectNetwork)
    String strSelectNetwork;
    @BindString(R.string.icons_eye_open)
    String iconEyeOpen;
    @BindString(R.string.icons_eye_slash)
    String iconEyeSlash;
    @BindString(R.string.dialogs_wifiNetworksDialog_title)
    String selectNetworkString;
    @BindString(R.string.dialogs_provisionWepWarning_title)
    String strProvisionBridgeWepWarning_title;
    @BindString(R.string.dialogs_provisionWepWarning_message)
    String strProvisionBridgeWepWarning_message;

    // Callback for when a network is validated (selected if no pass, and on password not blank for all others)
    public interface OnNetworkValidationChangedListener {
        void onNetworkValidationChanged(boolean isValid);
    }

    private OnNetworkValidationChangedListener onNetworkValidationChangedListener;

    // Callback for when a network is validated (selected if no pass, and on password not blank for all others)
    public interface OnNetworkSubmittedListener {
        void onNetworkSubmitted();
    }

    private OnNetworkSubmittedListener onNetworkSubmittedListener;

    private WifiManager wifiManager;
    private ScanResult bridgeScanResult;
    private List<ScanResult> scanResults = new ArrayList<>();
    private BroadcastReceiver wifiScanResultsBroadcastReceiver;

    private boolean isPasswordVisible = false;
    private boolean isSearchingForNetwork = false;
    private ScanResult currentNetworkScanResult;

    public SystemNetworkField(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        setMeasureAllChildren(false);

        // Inflate view/inject view members
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.view_field_system_network, this, true);
        ButterKnife.bind(this, view);

        // Add icons
        Iconify.addIcons(editIcon);

        // Submit once password is entered
        //TODO: Submit on network selection if network is open
        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Validation
                if (TextUtils.isEmpty(passwordEditText.getText().toString())) {
                    passwordErrorTextView.setVisibility(View.VISIBLE);
                    // Update field styles
                    passwordEditText.getBackground().setColorFilter(getResources().getColor(R.color.notion_red), PorterDuff.Mode.SRC_ATOP);
                    passwordEditText.requestFocus();
                    return true;
                }
                collapseField();
                if (onNetworkSubmittedListener != null) {
                    onNetworkSubmittedListener.onNetworkSubmitted();
                }
            }
            return false;
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Dispatch validation change if listener set
                if (onNetworkValidationChangedListener != null) {
                    onNetworkValidationChangedListener.onNetworkValidationChanged(isValid());
                }
            }
        });

        // Manually set the font styling on the password field for the hint text (android/calligraphy bug -- see https://github.com/chrisjenx/Calligraphy/issues/186)
        passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        // Probably a better way to create this guy, just snag it from one of the other fields for now
        passwordEditText.setTypeface(passwordErrorTextView.getTypeface());

        // Set password visible
        togglePasswordVisible(true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isSearchingForNetwork) {
            searchForNetwork();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (isSearchingForNetwork && wifiScanResultsBroadcastReceiver != null) {
            unregisterWifiScanBroadcastReceiver();
            wifiScanResultsBroadcastReceiver = null;
        }
    }

    public void unregisterWifiScanBroadcastReceiver() {
        getContext().unregisterReceiver(wifiScanResultsBroadcastReceiver);
    }

    public void setOnNetworkValidationChangedListener(OnNetworkValidationChangedListener onNetworkValidationChangedListener) {
        this.onNetworkValidationChangedListener = onNetworkValidationChangedListener;
    }

    public void setOnNetworkSubmittedListener(OnNetworkSubmittedListener onNetworkSubmittedListener) {
        this.onNetworkSubmittedListener = onNetworkSubmittedListener;
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility == VISIBLE && passwordContainer.getVisibility() == VISIBLE) {
            showSoftKeyboard();
        }
    }

    /**
     * Start searching for networks -- autoselect currently connected if available
     */
    public void searchForNetwork() {
        // Search in progress -- calm down already!
        if (isSearchingForNetwork && wifiScanResultsBroadcastReceiver != null) {
            return;
        }

        // Select wifi network currently connected to
        // Grab Wifi manager and turn wifi on if it already isn't
        wifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        // Select currently selected network
        ScanResult currentNetworkNon5GScanResult = NetworkUtils.getCurrentWifiConnectionScanResult(getContext());
        if (currentNetworkNon5GScanResult != null && !currentNetworkNon5GScanResult.SSID.isEmpty()) {
            setCurrentNetworkScanResult(currentNetworkNon5GScanResult);
        } else {
            setSelectNetworkView();
        }

        // Change to searching for bridge view
        //        showSearchingForBridgeView(); //TODO: SHow scanning text

        // register the BR
        registerWifiScanResultsReceiver();
        isSearchingForNetwork = wifiManager.startScan();
    }

    private void registerWifiScanResultsReceiver() {
        if (wifiScanResultsBroadcastReceiver != null) {
            return;
        }

        // Register the scan results BroadcastReceiver and then kick off a scan in the background
        wifiScanResultsBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                scanResults = NetworkUtils.filterOutBridgeNetworks(
                        NetworkUtils.filterOutEmptyNetworks(
                                NetworkUtils.filterOut5GNetworks(
                                        wifiManager.getScanResults())));
                isSearchingForNetwork = false;
            }
        };
        getContext().registerReceiver(wifiScanResultsBroadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    public void collapseField() {
        // Update the passwordTextView field in the overView
        // Can't use android:password="true" as it'll leave the last char unmasked (and intended for EditText)
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < passwordEditText.getText().length(); i++) {
            password.append("\u2022");
        }
        passwordTextView.setText(password.toString());
        setDisplayedChild(1);
    }

    public boolean isCollapsed() {
        return getDisplayedChild() == 1;
    }

    public void editField() {
        setDisplayedChild(0);
        if (currentNetworkScanResult != null && !currentNetworkScanResult.SSID.isEmpty() && !currentNetworkScanResult.SSID.equals(
                "<unknown ssid>")) {
            passwordEditText.requestFocus();
            ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(passwordEditText,
                                                                                                             InputMethodManager.SHOW_IMPLICIT);
        } else {
            passwordTextView.setVisibility(GONE);
            passwordEditText.setVisibility(GONE);
        }

    }

    public ScanResult getSelectedScanResult() {
        return currentNetworkScanResult;
    }

    public String getSSID() {
        if (currentNetworkScanResult != null) {
            return currentNetworkScanResult.SSID;
        }
        return "";
    }

    public String getPassword() {
        return passwordEditText.getText().toString();
    }

    public void setCurrentNetworkScanResult(ScanResult currentNetworkScanResult) {
        this.currentNetworkScanResult = currentNetworkScanResult;
        if (isWep()) {
            showWepWarning();
        }
        String capabilities = currentNetworkScanResult.capabilities;

        // Security check
        if (capabilities.contains("WPA/WPA2") || capabilities.contains("WPA2") || capabilities.contains("WPA") || capabilities.contains(
                "WEP")) {
            passwordTextView.setVisibility(VISIBLE);
            systemNetworkFieldPasswordLabelTextView.setVisibility(VISIBLE);
            passwordContainer.setVisibility(VISIBLE);
            this.postDelayed(() -> showSoftKeyboard(), 100);
        } else {
            passwordTextView.setVisibility(GONE);
            systemNetworkFieldPasswordLabelTextView.setVisibility(GONE);
            passwordContainer.setVisibility(GONE);
            passwordEditText.setText("");
        }
        // If need to filter on any security...not just those supported by the bridge, use this:
        //        String[] securityModes = {"WEP", "PSK", "EAP"};
        //        for (int i = securityModes.length - 1; i >= 0; i--) {
        //            if (capabilities.contains(securityModes[i])) {
        //                return securityModes[i];
        //            }
        //        }

        networkTextView.setText(currentNetworkScanResult.SSID);
        networkChooserTextView.setText(currentNetworkScanResult.SSID);
        isSearchingForNetwork = false;
    }

    private void setSelectNetworkView() {
        networkTextView.setText(strSelectNetwork);
        networkChooserTextView.setText(strSelectNetwork);

        passwordTextView.setVisibility(GONE);
        systemNetworkFieldPasswordLabelTextView.setVisibility(GONE);
        passwordContainer.setVisibility(GONE);
        passwordEditText.setText("");
    }

    public void setEditIconVisible(boolean isVisible) {
        View editIcon = findViewById(R.id.editIcon);
        if (editIcon != null) {
            editIcon.setVisibility(isVisible ? VISIBLE : INVISIBLE);
        }
    }

    /**
     * Whether or not the field is in a valid state. This means that if a network with a security type
     * is selected, a passphrase is supplied, and if a custom network is select, at least one character
     * is set for the SSID,
     */
    public boolean isValid() {
        if (currentNetworkScanResult == null) {
            return false;
        } else if (NetworkUtils.getSecurityType(currentNetworkScanResult) != NetworkUtils.SECURITY_NONE) {
            return !passwordEditText.getText().toString().isEmpty();
        }
        return true;
    }

    public boolean isWep() {
        if (currentNetworkScanResult == null) {
            return false;
        }
        return NetworkUtils.getSecurityType(currentNetworkScanResult) == NetworkUtils.SECURITY_WEP;
    }


    @OnClick(R.id.eyeIcon)
    public void eyeIconClicked() {
        togglePasswordVisible(!isPasswordVisible);
    }

    public void togglePasswordVisible(boolean isVisible) {
        // Get cursor position in passwordEditText as this click will remove focus
        int cursorPosition = passwordEditText.getSelectionStart(); // Handle actual selections maybe too?

        if (isVisible) {
            eyeIcon.setText(iconEyeOpen);
            eyeIcon.setTextColor(notionDarkTeal);
            passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordEditText.setTypeface(passwordErrorTextView.getTypeface());
        } else {
            eyeIcon.setText(iconEyeSlash);
            eyeIcon.setTextColor(notionMediumGrey);
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordEditText.setTypeface(passwordErrorTextView.getTypeface());
        }

        // Return cursor position
        passwordEditText.setSelection(cursorPosition);
        isPasswordVisible = isVisible;
    }

    /**
     * Show the soft keyboard if the password field is visible
     */
    public void showSoftKeyboard() {
        if (this.getVisibility() == VISIBLE && passwordContainer.getVisibility() == VISIBLE) {
            passwordEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) passwordEditText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(passwordEditText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Hide the soft keyboard for the password field if visible
     */
    public void hideSoftKeyboard() {
        if (passwordEditText.getVisibility() == VISIBLE) {
            InputMethodManager imm = (InputMethodManager) passwordEditText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm.isAcceptingText()) { // verify if the soft keyboard is open
                imm.hideSoftInputFromWindow(passwordEditText.getWindowToken(), 0);
            }
        }
    }

    /**
     * Show dialog to select the wifi network
     *
     * @param view view clicked
     */
    @OnClick(R.id.systemNetworkFieldNetworkContainer)
    public void selectNetwork(View view) {
        // Grab ssids
        final List<String> networkNames = new ArrayList<>(scanResults.size());
        for (ScanResult scanResult : scanResults) {
            networkNames.add(scanResult.SSID);
        }

        new AlertDialog.Builder(getContext())
                .setTitle(selectNetworkString)
                .setItems(networkNames.toArray(new CharSequence[networkNames.size()]),
                          (dialog, which) -> setCurrentNetworkScanResult(scanResults.get(which)))
                .create()
                .show();
    }

    public boolean isSearchingForNetwork() {
        return isSearchingForNetwork;
    }

    private void showWepWarning() {
        new SlideUpDialog.Builder()
                .colorTheme(SlideUpDialog.ColorTheme.RED)
                .title(strProvisionBridgeWepWarning_title)
                .message(strProvisionBridgeWepWarning_message)
                .confirmTitle("Go to Knowledge Base")
                .confirmClickListener(dialog -> {
                    Intent goToKnowledgeBase = new Intent(Intent.ACTION_VIEW,
                                                          Uri.parse("http://support.getnotion.com/hc/en-us/articles/235950248d"));
                    context.startActivity(goToKnowledgeBase);
                })
                .cancelTitle("Cancel")
                .cancelClickListener(Dialog::dismiss)
                .build(context)
                .show();
    }
}