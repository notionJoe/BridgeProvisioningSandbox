package com.getnotion.android.bridgeprovisioner;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.getnotion.android.bridgeprovisioner.R;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import com.getnotion.android.bridgeprovisioner.models.NotionBridge;
import com.getnotion.android.bridgeprovisioner.models.NotionSystem;
import com.getnotion.android.bridgeprovisioner.models.wrappers.BridgePostResponse;
import com.getnotion.android.bridgeprovisioner.models.wrappers.BridgeRequest;
import com.getnotion.android.bridgeprovisioner.network.RestClient;
import com.getnotion.android.bridgeprovisioner.network.bridge.provision.BridgeConfig;
import com.getnotion.android.bridgeprovisioner.network.bridge.provision.BridgeConstants;
import com.getnotion.android.bridgeprovisioner.network.bridge.provision.BridgeUtils;
import com.getnotion.android.bridgeprovisioner.utils.SystemUtils;
import com.getnotion.android.bridgeprovisioner.views.CollapsibleField;
import com.getnotion.android.bridgeprovisioner.views.PermissionsField;
import com.getnotion.android.bridgeprovisioner.views.SlideUpDialog;
import com.getnotion.android.bridgeprovisioner.views.SuccessDialogFragment;
import com.getnotion.android.bridgeprovisioner.views.SystemBridgeField;
import com.getnotion.android.bridgeprovisioner.views.SystemNameField;
import com.getnotion.android.bridgeprovisioner.views.SystemNetworkField;

import java.util.TimeZone;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.BindString;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;
import retrofit.Callback;
import retrofit.RetrofitError;

public class AddNewBridgeActivity extends AppCompatActivity implements SystemBridgeField.OnBridgeSelectedListener,
                                                                       SystemBridgeField.OnBridgeNotFoundListener,
                                                                       SystemNetworkField.OnNetworkSubmittedListener,
                                                                       SystemNetworkField.OnNetworkValidationChangedListener {

    private static final String TAG = AddNewBridgeActivity.class.getSimpleName();

    // Intent actions for configuring this activity on start
    public static final String ACTION_CREATE_SYSTEM = TAG + ".ACTION_CREATE_SYSTEM";
    public static final String ACTION_ADD_BRIDGE = TAG + ".ACTION_ADD_BRIDGE";
    public static final String ACTION_PROVISION_BRIDGE = TAG + ".ACTION_PROVISION_BRIDGE";

    // Request code starting activity for the user to change WRITE_SETTINGS
    public static final int WRITE_SETTINGS_PERMISSIONS_REQUEST = 0;

    // Container-level views
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.addNewBridgeScrollView)
    ScrollView addNewBridgeScrollView;
    @Bind(R.id.busyOverlay)
    View busyOverlay;

    // Collapsible Fields
    @Bind(R.id.systemNameField)
    SystemNameField systemNameField;
    @Bind(R.id.permissionsField)
    PermissionsField permissionsField;
    @Bind(R.id.bridgeField)
    SystemBridgeField bridgeField;
    @Bind(R.id.networkField)
    SystemNetworkField networkField;

    // ProgressButton Views
    @Bind(R.id.addNewBridgeFooterButtonContainer)
    View footerButtonContainer;
    @Bind(R.id.addNewBridgeFooterButton)
    Button footerButton;
    @Bind(R.id.addNewBridgeProgressBar)
    ProgressBar progressBar;

    // FooterButton Strings
    @BindString(R.string.global_next)
    String strNext;
    @BindString(R.string.setup_system_progressButton_searchButton)
    String strSearchForBridge;
    @BindString(R.string.setup_system_progressButton_searchButtonActive)
    String strSearchingForBridge;
    @BindString(R.string.setup_system_progressButton_addNetwork)
    String strAddNetwork;
    @BindString(R.string.setup_system_addSystemButton_connectingToBridge)
    String strConnectingToBridge;
    @BindString(R.string.setup_system_addSystemButton_provisioningBridge)
    String strConfiguringBridge;
    @BindString(R.string.setup_system_progressButton_createSystem)
    String strCreateSystem;
    @BindString(R.string.setup_system_progressButton_addBridge)
    String strAddBridge;
    @BindString(R.string.setup_system_progressButton_configureBridge)
    String strConfigureBridge;
    @BindString(R.string.setup_system_addSystemButton_addingSystem)
    String strCreatingSystem;
    @BindString(R.string.setup_system_addSystemButton_addingBridge)
    String strAddingBridge;

    @BindString(R.string.dialogs_provisionBridgeError_messageFailedToConnectGeneric)
    String strFailedToConnectToBridgeGeneric;
    @BindString(R.string.dialogs_provisionBridgeError_messageAuthenticationError)
    String strFailedToConnectToBridgeAuth;
    @BindString(R.string.dialogs_provisionBridgeError_message)
    String strFailedToProvisionBridge;
    @BindString(R.string.dialogs_provisionBridgeError_title)
    String strProvisionBridgeErrorTitle;

    // Success Strings
    @BindString(R.string.successScreen_systemCreated_title)
    String strSuccessCreatedSystemTitle;
    @BindString(R.string.successScreen_systemCreated_message)
    String strSuccessCreatedSystemMessage;
    @BindString(R.string.successScreen_bridgeAdded_title)
    String strSuccessAddedBridgeTitle;
    @BindString(R.string.successScreen_bridgeAdded_message)
    String strSuccessAddedBridgeMessage;
    @BindString(R.string.successScreen_bridgeConfigured_title)
    String strSuccessConfiguredBridgeTitle;
    @BindString(R.string.successScreen_bridgeConfigured_message)
    String strSuccessConfigureBridgeMessage;

    //
    @BindString(R.string.title_addNewBridgeActivity_addNewBridge)
    String strTitleAddNewBridge;
    @BindString(R.string.title_addNewBridgeActivity_createSystem)
    String strTitleCreateSystem;
    @BindString(R.string.title_addNewBridgeActivity_provisionBridge)
    String strTitleProvisionBridge;

    // Colors
    @BindColor(R.color.notion_teal)
    int notionTeal;
    @BindColor(R.color.notion_white)
    int notionWhite;
    @BindColor(R.color.notion_dark_teal)
    int notionDarkTeal;
    @BindColor(R.color.notion_dark_grey)
    int notionDarkGrey;
    @BindColor(R.color.notion_light_grey)
    int notionLightGrey;


    // Setup/Action States
    private int currentState = STATE_START;
    private static final int STATE_START = 0;
    private static final int STATE_INPUT_SYSTEM_NAME = 1;
    private static final int STATE_ENABLE_PERMISSIONS = 2;
    private static final int STATE_BRIDGE_NOT_FOUND = 3;
    private static final int STATE_SELECT_BRIDGE = 4;
    private static final int STATE_SELECT_NETWORK = 5;
    private static final int STATE_SELECT_NETWORK_AUTH_FAILED = 6;
    private static final int STATE_CREATE_SYSTEM = 7;

    // Wait States
    private static final int STATE_SEARCH_FOR_BRIDGE = 8;
    private static final int STATE_PROVISION_BRIDGE = 9;
    private static final int STATE_CREATING_SYSTEM = 10;


    // Misc state flags and location fields
    private boolean isReviewingInputs = false; // for editing after showing all fields
    private boolean isBridgeProvisioned = false;
    private boolean bridgeProvisioningFailed = false;
    private boolean systemAdded = false;
    private boolean hasCheckedForPermissions = false;
    private double latitude = 39.74;
    private double longitude = -104.99;

    // Intent and Receiver for the ProvisionBridgeService
    private BroadcastReceiver provisionBridgeBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_bridge);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        // Set default action to provision bridge
        if (getIntent().getAction() == null) {
            getIntent().setAction(ACTION_PROVISION_BRIDGE);
        }

        setupUI();
        attachListeners();

        // Set the current state and get this machine moovin'
        currentState = STATE_START;
        processState();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (getIntent().getAction().equals(ACTION_CREATE_SYSTEM)) {
            overridePendingTransition(R.anim.stay_put, R.anim.slide_out_right);
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (provisionBridgeBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(provisionBridgeBroadcastReceiver);
            provisionBridgeBroadcastReceiver = null;
        }
    }

    /**
     * Configure UI - Skin drawables, set visibility & such
     */
    private void setupUI() {
        // Set title based off action
        String title = toolbar.getTitle().toString();
        if (getIntent().getAction().equals(ACTION_CREATE_SYSTEM)) {
            title = strTitleCreateSystem;
        } else if (getIntent().getAction().equals(ACTION_ADD_BRIDGE)) {
            title = strTitleAddNewBridge;
        } else if (getIntent().getAction().equals(ACTION_PROVISION_BRIDGE)) {
            title = strTitleProvisionBridge;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        // Set progress bar color
        progressBar.getIndeterminateDrawable().setColorFilter(notionDarkTeal, android.graphics.PorterDuff.Mode.MULTIPLY);

        // Hide edit icons for home/permissions to keep things simple
        systemNameField.setEditIconVisible(false);
        permissionsField.setEditIconVisible(false);
        bridgeField.setEditIconVisible(false);
        networkField.setEditIconVisible(false);
    }

    /**
     * Attach listeners to permissions, bridge, and network fields and the busy overlay
     */
    private void attachListeners() {
        // Attach listener for when the SystemName is submitted
        systemNameField.setOnFieldSubmittedListener(collapsibleField -> processState());

        // Ask for permission once location permission switch is checked
        permissionsField.setLocationPermissionOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                LocationPermissionsHelper.checkForLocationPermissions(this, hasCheckedForPermissions);
                hasCheckedForPermissions = true;
            }
            setFooterButtonEnabled(permissionsField.getPermissionsEnabled());
        });

        // This field is only visible if version is 6.0.0 -- see permissionsField
        permissionsField.setWriteSettingsPermissionsOnCheckedChangeListener((buttonView, isChecked) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent writeSettingsIntent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                writeSettingsIntent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(writeSettingsIntent, WRITE_SETTINGS_PERMISSIONS_REQUEST);
                // See onActivityResult() for footer button update
            }
        });

        // Listeners for when a bridge is found/selected
        bridgeField.setOnBridgeSelectedListener(this);
        bridgeField.setOnBridgeNotFoundListener(this);

        // Listeners for when a network is found/selected
        networkField.setOnNetworkValidationChangedListener(this);
        networkField.setOnNetworkSubmittedListener(this);

        // Configure busyOverlay to consume all touch events
        busyOverlay.setOnTouchListener((v, event) -> true);
    }

    /**
     * Performs the required action for whatever the current state is
     * <p/>
     * If re-entering a state, set currentState to be the state before the
     * one you want for proper field setup. These behave as submission-states
     * as opposed to entry-states.
     * <p/>
     * / Break out into individual methods if this becomes too much /
     */
    @OnClick(R.id.addNewBridgeFooterButton)
    public void processState() {
        switch (currentState) {
            case STATE_START: {
                currentState = STATE_INPUT_SYSTEM_NAME;
                if (getIntent().getAction().equals(ACTION_CREATE_SYSTEM)) {
                    systemNameField.setVisibility(View.VISIBLE);
                    updateFooterButton();
                } else {
                    processState();
                }
                break;
            }
            case STATE_INPUT_SYSTEM_NAME: {
                onFieldSubmitted(systemNameField);
                // If marshmallow+ show permissionsField, otherwise show bridgeField
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    permissionsField.setVisibility(View.VISIBLE);
                    // If permissions are already enabled collapse the field and move on
                    if (!LocationPermissionsHelper.arePermissionsEnabled(this)) {
                        currentState = STATE_ENABLE_PERMISSIONS;
                    } else {
                        onFieldSubmitted(permissionsField);
                        bridgeField.setVisibility(View.VISIBLE);
                        currentState = STATE_SEARCH_FOR_BRIDGE;
                        processState();
                    }
                } else {
                    bridgeField.setVisibility(View.VISIBLE);
                    currentState = STATE_SEARCH_FOR_BRIDGE;
                    processState();
                }
                updateFooterButton();

                break;
            }
            case STATE_ENABLE_PERMISSIONS: {
                // One last permissions check in case something was disabled in the background
                // User a SettingContentObserver for a more robust solution: http://stackoverflow.com/a/7017516
                if (!LocationPermissionsHelper.arePermissionsEnabled(this)) {
                    Toast.makeText(AddNewBridgeActivity.this, "Permissions must be enabled to continue.", Toast.LENGTH_SHORT).show();
                    break;
                }

                onFieldSubmitted(permissionsField);
                bridgeField.setVisibility(View.VISIBLE);

                currentState = STATE_SEARCH_FOR_BRIDGE;
                updateFooterButton();
                processState();
                break;
            }
            case STATE_SEARCH_FOR_BRIDGE: {
                bridgeField.searchForBridge();
                networkField.searchForNetwork();
                fetchLocation();
                break;
            }
            case STATE_BRIDGE_NOT_FOUND: {
                currentState = STATE_SEARCH_FOR_BRIDGE;
                updateFooterButton();
                processState();
                break;
            }
            case STATE_SELECT_BRIDGE: {
                onBridgeSubmitted();
                networkField.setVisibility(View.VISIBLE);
                addNewBridgeScrollView.post(() -> { // Will show the softkeyboard is the password field is visible
                    networkField.showSoftKeyboard();
                    addNewBridgeScrollView
                            .postDelayed(() -> addNewBridgeScrollView.smoothScrollTo(0, addNewBridgeScrollView.getBottom()), 100);
                });
                currentState = STATE_SELECT_NETWORK;
                updateFooterButton();
                break;
            }
            case STATE_SELECT_NETWORK: {
                onNetworkSubmitted();
                if (!bridgeProvisioningFailed) {
                    currentState = STATE_PROVISION_BRIDGE;
                    updateFooterButton();
                    processState();
                } else {
                    bridgeProvisioningFailed = false;
                    updateFooterButton();
                }
                break;
            }
            case STATE_SELECT_NETWORK_AUTH_FAILED: {
                networkField.editField();
                addNewBridgeScrollView.post(() -> { // Will show the softkeyboard is the password field is visible
                    networkField.showSoftKeyboard();
                    addNewBridgeScrollView.postDelayed(() -> addNewBridgeScrollView.smoothScrollTo(0, addNewBridgeScrollView.getBottom()),
                                                       100);
                });
                currentState = STATE_SELECT_NETWORK;
                updateFooterButton();
                break;
            }
            case STATE_PROVISION_BRIDGE: {
                provisionBridge();
                break;
            }
            case STATE_CREATE_SYSTEM: {
                if (getIntent().getAction().equals(ACTION_CREATE_SYSTEM)) {
                    createSystemAndBaseStation();
                } else if (getIntent().getAction().equals(ACTION_ADD_BRIDGE)) {
                    createBridge();
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    /**
     * Updates the footer button based off the current state
     */
    private void updateFooterButton() {
        switch (currentState) {
            case STATE_INPUT_SYSTEM_NAME: {
                footerButton.setText(strNext);
                setFooterButtonEnabled(systemNameField.isValid());
                break;
            }
            case STATE_ENABLE_PERMISSIONS: {
                footerButton.setText(strSearchForBridge);
                progressBar.setVisibility(View.GONE);
                setFooterButtonEnabled(permissionsField.isValid());
                break;
            }
            case STATE_SEARCH_FOR_BRIDGE: {
                footerButton.setText(strSearchingForBridge);
                setFooterButtonEnabled(false);
                break;
            }
            case STATE_BRIDGE_NOT_FOUND: {
                footerButton.setText(strSearchForBridge);
                setFooterButtonEnabled(true);
                break;
            }
            case STATE_SELECT_BRIDGE: {
                footerButton.setText(strAddNetwork);
                progressBar.setVisibility(View.GONE);
                setFooterButtonEnabled(true); //TODO: Only set enabled if bridge is valid
                break;
            }
            case STATE_SELECT_NETWORK: {
                footerButton.setText(strConfigureBridge);
                setFooterButtonEnabled(networkField.isValid());
                break;
            }
            case STATE_SELECT_NETWORK_AUTH_FAILED: {
                footerButton.setText(strConfigureBridge);
                setFooterButtonEnabled(networkField.isValid());
                break;
            }
            case STATE_PROVISION_BRIDGE: {
                footerButton.setText(strConfiguringBridge);
                setFooterButtonEnabled(false);
                break;
            }
            case STATE_CREATE_SYSTEM: {
                if (getIntent().getAction().equals(ACTION_CREATE_SYSTEM)) {
                    footerButton.setText(strCreateSystem);
                } else if (getIntent().getAction().equals(ACTION_ADD_BRIDGE)) {
                    footerButton.setText(strAddBridge);
                }
                setFooterButtonEnabled(true);
                break;
            }
            case STATE_CREATING_SYSTEM: {
                if (getIntent().getAction().equals(ACTION_CREATE_SYSTEM)) {
                    footerButton.setText(strCreatingSystem);
                } else if (getIntent().getAction().equals(ACTION_ADD_BRIDGE)) {
                    footerButton.setText(strAddingBridge);
                }
                setFooterButtonEnabled(false);
                break;
            }
            default: {
                footerButtonContainer.setVisibility(View.GONE);
                break;
            }
        }
    }

    /**
     * Enables/disables the footer button and updates styles accordingly
     *
     * @param isEnabled whether to enable the button or not
     */
    private void setFooterButtonEnabled(boolean isEnabled) {
        // To prevent animating valid again
        if (footerButton.isEnabled() == isEnabled) {
            return;
        }

        if (isEnabled) {
            ObjectAnimator.ofObject(footerButton, "backgroundColor", new ArgbEvaluator(), notionLightGrey, notionTeal)
                          .setDuration(500)
                          .start();
            footerButton.setTextColor(notionWhite);
        } else {
            ObjectAnimator.ofObject(footerButton, "backgroundColor", new ArgbEvaluator(), notionTeal, notionLightGrey)
                          .setDuration(500)
                          .start();
            footerButton.setTextColor(notionDarkGrey);
        }
        footerButton.setEnabled(isEnabled);
    }

    private void onFieldSubmitted(CollapsibleField field) {
        field.collapseField();
        field.setBackgroundColor(notionLightGrey);
    }

    // TODO: Refactor SystemBridgeField to be a CollapsibleField
    private void onBridgeSubmitted() {
        bridgeField.collapseField();
        bridgeField.setBackgroundColor(notionLightGrey);
    }

    public void onAllFieldsSubmitted() {
        isReviewingInputs = true;
        systemNameField.setBackgroundColor(notionWhite);
        permissionsField.setBackgroundColor(notionWhite);
        bridgeField.setBackgroundColor(notionWhite);
        networkField.setBackgroundColor(notionWhite);
    }

    /**
     * All information provided, now connect, configure, and disconnected from bridge
     */
    private void provisionBridge() {
        // Hide the soft keyboard if still visible (would be the case when editing a field and click 'Create account'
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }

        // Create BridgeConfig object for provisioning the bridge
        BridgeConfig bridgeConfig = new BridgeConfig();
        bridgeConfig.setBridgeSSID(bridgeField.getBridgeScanResult().SSID);
        bridgeConfig.setNetworkFieldsFromScanResult(networkField.getSelectedScanResult());
        bridgeConfig.setNetworkPassword(networkField.getPassword());

        // Create intent and start the BridgeProvisioningService
        Intent provisionBridgeServiceIntent = new Intent(AddNewBridgeActivity.this, BridgeProvisioningService.class);
        provisionBridgeServiceIntent.putExtra(BridgeProvisioningService.BRIDGE_CONFIG, bridgeConfig);
        startService(provisionBridgeServiceIntent);

        registerProvisionBridgeBroadcastReceiver();
    }

    private void createSystemAndBaseStation() {
        currentState = STATE_CREATING_SYSTEM;
        updateFooterButton();

        if (systemAdded) {
            createBridge();
            return;
        }

        NotionSystem notionSystem = new NotionSystem(
                systemNameField.getSystemName(),
                TimeZone.getDefault().getID(),
                latitude,
                longitude);

        RestClient.notionApi()
                  .createSystem(new NotionSystem.SystemRequest(notionSystem),
                                new Callback<NotionSystem.SystemPOSTResponse>() {
                                    @Override
                                    public void success(NotionSystem.SystemPOSTResponse systemPOSTResponse, retrofit.client.Response response) {
                                        systemAdded = true;

                                        // Persist account to local cache
                                        Realm realm = Realm.getDefaultInstance();
                                        realm.beginTransaction();
                                        realm.copyToRealm(systemPOSTResponse.getSystems());
                                        realm.commitTransaction();
                                        realm.close();

                                        createBridge();
                                    }

                                    @Override
                                    public void failure(RetrofitError error) {
                                        currentState = AddNewBridgeActivity.STATE_CREATE_SYSTEM;
                                        updateFooterButton();
                                        setFooterButtonEnabled(true);

                                        // Get appropriate error message for response
                                        String errorMessage = SystemUtils.getErrorMessageForResponse(AddNewBridgeActivity.this, error);

                                        Log.e(TAG, error.getMessage());
                                        new SlideUpDialog.OkBuilder()
                                                .colorTheme(SlideUpDialog.ColorTheme.RED)
                                                .title(AddNewBridgeActivity.this
                                                               .getString(R.string.dialogs_systemSetupError_title))
                                                .message(errorMessage)
                                                .build(AddNewBridgeActivity.this)
                                                .show();
                                    }
                                });
    }

    private void createBridge() {
        currentState = STATE_CREATING_SYSTEM;
        updateFooterButton();

        NotionBridge bridge = new NotionBridge(
                bridgeField.getBridgeHardwareId(), 392);

        RestClient.notionApi()
                  .createBridge(new BridgeRequest(bridge),
                                new Callback<BridgePostResponse>() {
                                    @Override
                                    public void success(BridgePostResponse bridgePostResponse, retrofit.client.Response response) {
                                        // Persist base_station to local cache
                                        Realm realm = Realm.getDefaultInstance();
                                        realm.beginTransaction();
                                        realm.copyToRealmOrUpdate(bridgePostResponse.getBridge());
                                        realm.commitTransaction();
                                        realm.close();

                                        if (getIntent().getAction().equals(ACTION_CREATE_SYSTEM)) {
                                            setResult(RESULT_OK);
                                            finish();
                                        } else if (getIntent().getAction().equals(ACTION_ADD_BRIDGE)) {
                                            showSuccessDialog(strSuccessAddedBridgeTitle, strSuccessAddedBridgeMessage);
                                        }
                                    }

                                    @Override
                                    public void failure(RetrofitError error) {
                                        currentState = AddNewBridgeActivity.STATE_CREATE_SYSTEM;
                                        updateFooterButton();
                                        setFooterButtonEnabled(true);

                                        // Get appropriate error message for response
                                        String errorMessage = BridgeUtils
                                                .getErrorMessageForResponse(AddNewBridgeActivity.this, error);

                                        Log.e(TAG, error.getMessage());
                                        new SlideUpDialog.OkBuilder()
                                                .colorTheme(SlideUpDialog.ColorTheme.RED)
                                                .title(AddNewBridgeActivity.this
                                                               .getString(R.string.dialogs_addingBridgeError_title))
                                                .message(errorMessage)
                                                .build(AddNewBridgeActivity.this)
                                                .show();
                                    }
                                }

                               );
    }

    /**
     * Set the local instance of the ProvisionBridgeBR and register it with the LocalBroadcastManager
     */
    private void registerProvisionBridgeBroadcastReceiver() {
        if (provisionBridgeBroadcastReceiver != null) {
            return;
        }

        provisionBridgeBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String statusMessage = "[No message supplied :(]";
                boolean statusState = true;
                int resultCode = -1;

                // Grab extras, including message, end result, and result code
                if (intent.getExtras() != null) {
                    statusMessage = intent.getExtras().getString(BridgeProvisioningService.PROVISIONING_MESSAGE);
                    statusState = intent.getExtras().getBoolean(BridgeProvisioningService.RESULT);
                    resultCode = intent.getExtras().getInt(BridgeProvisioningService.RESULT_CODE);
                }

                // Status message update
                if (intent.getAction().equals(BridgeProvisioningService.MESSAGE_BROADCAST)) {
                    footerButton.setText(statusMessage);
                }

                // Provisioning state update
                if (intent.getAction().equals(BridgeProvisioningService.RESULT_BROADCAST)) {
                    if (statusState) {
                        // Only reconfiguring a bridge, bail!
                        if (getIntent().getAction().equals(ACTION_PROVISION_BRIDGE)) {
                            showSuccessDialog(strSuccessConfiguredBridgeTitle, strSuccessConfigureBridgeMessage);
                            return;
                        }

                        isBridgeProvisioned = true;

                        // Badge the bridge field as being provisioned in case network call to add it fails
                        bridgeField.setBridgeProvisioned(true);
                        bridgeField.setEditIconVisible(true);

                        currentState = AddNewBridgeActivity.STATE_CREATE_SYSTEM;
                        processState();
                    } else {
                        currentState = AddNewBridgeActivity.STATE_SELECT_NETWORK;

                        // Get the appropriate error message
                        if (resultCode == BridgeConstants.Results.RESULT_CODE_CONNECTION_FAILURE) {
                            statusMessage = strFailedToConnectToBridgeGeneric;
                        } else if (resultCode == BridgeConstants.Results.RESULT_CODE_CONNECTION_FAILURE_AUTH) {
                            statusMessage = strFailedToConnectToBridgeAuth;
                            currentState = AddNewBridgeActivity.STATE_SELECT_NETWORK_AUTH_FAILED;
                        } else {
                            statusMessage = strFailedToProvisionBridge;
                        }

                        bridgeProvisioningFailed = true;

                        new SlideUpDialog.OkBuilder()
                                .colorTheme(SlideUpDialog.ColorTheme.RED)
                                .title(strProvisionBridgeErrorTitle)
                                .message(statusMessage)
                                .confirmClickListener(dialog -> {
                                    dialog.dismiss();
                                    processState();
                                })
                                .build(AddNewBridgeActivity.this)
                                .show();
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(BridgeProvisioningService.RESULT_BROADCAST);
        intentFilter.addAction(BridgeProvisioningService.MESSAGE_BROADCAST);
        LocalBroadcastManager.getInstance(this).registerReceiver(provisionBridgeBroadcastReceiver, intentFilter);
    }

    private void showSuccessDialog(String title, String message) {
        SuccessDialogFragment.Builder dialogBuilder = new SuccessDialogFragment.Builder()
                .title(title)
                .message(message)
                .confirmTitle(getResources().getString(R.string.successScreen_confirm))
                .onClickListener((dialog, which) -> {
                    AddNewBridgeActivity.this.setResult(RESULT_OK);
                    finish();
                });
        dialogBuilder.build().show(getSupportFragmentManager(), SuccessDialogFragment.TAG);
    }

    /**
     * For reacting to write_settings permission changes
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == WRITE_SETTINGS_PERMISSIONS_REQUEST) {
            permissionsField.refreshWriteSettingsSwitch();
            setFooterButtonEnabled(permissionsField.getPermissionsEnabled());
        }
    }

    //<editor-fold desc="Location Methods">

    /**
     * Grab the users current location and store them in the longitude/latitude fields
     */
    private void fetchLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        } else {
            Log.e(TAG, "Location is null -- work NAPA-148 already!");
        }
    }

    @Override
    public void onBridgeNotFound() {
        currentState = STATE_BRIDGE_NOT_FOUND;
        updateFooterButton();
        // no need to call processState(), these are UI updates, not field submission states
    }

    @Override
    public void onBridgeSelected() {
        currentState = STATE_SELECT_BRIDGE;
        updateFooterButton();
        // no need to call processState(), these are UI updates, not field submission states
    }

    @Override
    public void onNetworkSubmitted() {
        networkField.collapseField();
        onAllFieldsSubmitted();

    }

    @Override
    public void onNetworkValidationChanged(boolean isValid) {
        setFooterButtonEnabled(isValid);
    }
}
