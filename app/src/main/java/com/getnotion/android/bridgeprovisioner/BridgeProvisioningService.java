package com.getnotion.android.bridgeprovisioner;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.getnotion.android.bridgeprovisioner.network.bridge.provision.BridgeConstants;
import com.getnotion.android.bridgeprovisioner.network.bridge.provision.IProvisioningStatusListener;
import com.getnotion.android.bridgeprovisioner.network.bridge.provision.BridgeConfig;
import com.getnotion.android.bridgeprovisioner.network.bridge.provision.BridgeProvisioner;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for provisioning a NotionBridge. It performs the following actions
 * <p/>
 * -Connect to Bridge
 * -Begin Provision Bridge
 * -Provision Bridge Error/Success
 * -Disconnect from Bridge
 */
public class BridgeProvisioningService extends Service implements IProvisioningStatusListener {

    private static final String TAG = BridgeProvisioningService.class.getSimpleName();

    public static final String BRIDGE_CONFIG = TAG + ".BRIDGE_CONFIG"; // Key for 'buildConfig' extra
    public static final String RESULT_BROADCAST = TAG + ".RESULT_BROADCAST"; // Intent action
    public static final String MESSAGE_BROADCAST = TAG + ".MESSAGE_BROADCAST";
    public static final String PROVISIONING_MESSAGE = TAG + ".STATUS_MESSAGE"; // Key for 'status' extra

    public static final String RESULT = TAG + ".RESULT";
    public static final String RESULT_CODE = TAG + ".RESULT_CODE";

    private static final int CONNECTING = 0;
    private static final int PROVISIONING_SECURE = 1;
    private static final int PROVISIONING_INSECURE = 2;
    private static final int DISCONNECTING = 3;

    private static int CURRENT_STATE = CONNECTING;

    private BridgeConfig bridgeConfig;
    private CountDownTimer countDownTimer;
    private ConnectionReceiver connectionReceiver;
    private WifiManager wifiManager;
    private int networkId;

    private boolean wasSuccessful = false;
    private boolean wasAuthFailure = false;

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private ConnectivityManager connectivityManager;

    private AtomicBoolean receiverRegistered = new AtomicBoolean(false);
    private BridgeProvisioner bridgeProvisioner;

    @Override
    public void onCreate() {
        Log.d(TAG, "Service created");
        Log.d(TAG, "Creating new thread, and handler");
        HandlerThread thread = new HandlerThread(TAG, Process.THREAD_PRIORITY_MORE_FAVORABLE);
        thread.start();

        // Grab the threads looper to use for our handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting service...");
        CURRENT_STATE = CONNECTING;

        if (intent.getExtras() != null) {
            bridgeConfig = intent.getExtras().getParcelable(BRIDGE_CONFIG);
        }
        if (bridgeConfig != null && bridgeConfig.isValid()) {
            // Be sure to send id w/ message so we know which to stop
            Message msg = serviceHandler.obtainMessage();
            msg.arg1 = startId;
            serviceHandler.sendMessage(msg);
        } else {
            Log.d(TAG, "Service started with invalid build config");
            Log.d(TAG, "Stopping service...");
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");

        NotionBridgeProvisionerApplication.getRefWatcher(this).watch(this);

        CURRENT_STATE = CONNECTING;
        serviceLooper = null;
        serviceHandler = null;
        wifiManager = null;
        connectivityManager = null;
        bridgeConfig = null;
        connectionReceiver = null;

        bridgeProvisioner.tearDown();
        bridgeProvisioner = null;

        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        stopSelf();
    }


    // Handler to receive messages from  thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (CURRENT_STATE) {
                case CONNECTING:
                    connectToBridge();
                    break;
                case PROVISIONING_INSECURE:
                    provisionBridge(false);
                    break;
                case PROVISIONING_SECURE:
                    provisionBridge(true);
                    break;
                case DISCONNECTING:
                    disconnectFromBridge();
                    break;
            }
        }
    }

    private class ConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Connection change received:");
            Log.d(TAG, "\taction=" + intent.getAction());

            for (String extra : intent.getExtras().keySet()) {
                Log.d(TAG, "\t\t" + extra);
            }

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            Log.d(TAG, "\tSSID: " + wifiInfo.getSSID());

            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                Log.d(TAG, "\tTypeName: " + info.getTypeName());
                Log.d(TAG, "\tisconnected: " + info.isConnected());
            }
        }
    }

    /**
     * Initiate connection with bridge.
     */
    private void connectToBridge() {
        Log.d(TAG, "Connecting to Bridge...");
        broadcastMessage("Connecting to Bridge...");

        countDownTimer = new CountDownTimer(10000, 2000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "connectToBridge() tick tick..." + millisUntilFinished + " remaining");
                if (isConnectedToWifi()) {
                    wifiManager.disconnect();
                }
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "connectToBridge() Time EXPIRED!");
                if (isConnectedToBridge()) {
                    // Possibly throws a SecurityException down the chain via ConnectivityManager
                    // See MOB-16 -- this is the most convenient place to catch
                    try {
                        onConnectedToBridge();
                    } catch (SecurityException e) {
                        Log.e(TAG, "Failed to connect to Bridge -- invalid permissions.\n" + e);
                        broadcastMessage("Failed to connect to Bridge -- invalid permissions.");
                        broadcastResult(false, BridgeConstants.Results.RESULT_CODE_CONNECTION_FAILURE);
                        unregisterConnectionReceiver(connectionReceiver);
                        updateState(CONNECTING);
                    }
                } else {
                    Log.e(TAG, "Failed to connect to Bridge...");
                    broadcastMessage("Failed to connect to Bridge...");
                    broadcastResult(false, BridgeConstants.Results.RESULT_CODE_CONNECTION_FAILURE);
                    wifiManager.removeNetwork(networkId);
                    wifiManager.saveConfiguration();

                    //TODO: Temp fix for receiver not being connected (not necessarily null in certain cases as well)
                    try {
                        unregisterConnectionReceiver(connectionReceiver);
                    } catch (Exception e) {
                        Log.e(TAG, "Error un-registering receiver on failure to connect to bridge", e);
                    } finally {
                        updateState(CONNECTING);
                    }
                }
            }
        };
        countDownTimer.start();
        registerConnectionReceiver();
        connectToBridgeAccessPoint();
    }

    private void connectToBridgeAccessPoint() {
        networkId = getBridgeNetworkIdFromWifiManager();
        if (networkId == -1) {
            final WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + bridgeConfig.getBridgeSSID() + "\"";
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            conf.priority = 999999;
            networkId = wifiManager.addNetwork(conf);
            Log.d(TAG, "Added Bridge Network to WifiManager. NetworkID: " + networkId);
            wifiManager.disconnect();
        }

        boolean enabledNetwork = wifiManager.enableNetwork(networkId, true);
        Log.d(TAG, "Enabled Bridge Network: " + enabledNetwork);

    }

    /**
     * Successfully connected to the bridge. Set up dedicated connection if L+ and start provisioning
     * <p>
     * WARNING: ConnectivityManager can throw a security exception on 6.0.0 if WRITE_PERMISSIONS
     * aren't enabled. See added permissions in manifest for details.
     */
    private void onConnectedToBridge() {
        countDownTimer.cancel();
        Log.d(TAG, "Connected to Bridge: " + bridgeConfig.getBridgeSSID());

        try {
            // Temp fix for crash during provisioning -- race condition?
            unregisterConnectionReceiver(connectionReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Cannot remove receiver after successfully connecting to bridge", e);
        }

        // If we're on lollipop+, make sure we bind this process to the bridge network so network
        // calls aren't relayed through a network configuration w/ internet access -- see docs for
        // {WifiManager#enabledNetwork}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d(TAG, "SDK >= Marshmallow -- Binding process to network");
                NetworkRequest.Builder req = new NetworkRequest.Builder();
                req.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
                connectivityManager.requestNetwork(req.build(), new ConnectivityManager.NetworkCallback() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onAvailable(Network network) {
                        ConnectivityManager.setProcessDefaultNetwork(network);
                    }

                    @SuppressLint("NewApi")
                    @Override
                    public void onLost(Network network) {
                        ConnectivityManager.setProcessDefaultNetwork(null);
                    }
                });
            }
        }

        // Switch to provisioning
        updateState(PROVISIONING_SECURE);
        Message msg = serviceHandler.obtainMessage();
        serviceHandler.handleMessage(msg);
    }

    /**
     * Provision the bridge
     */
    private void provisionBridge(boolean provisionSecurely) {
        Log.d(TAG, "Provisioning Bridge -- Secure:" + provisionSecurely);
        broadcastMessage("Configuring Bridge...");

        bridgeProvisioner = BridgeProvisioner.getInstance();
        bridgeProvisioner.provisionBridge(this, provisionSecurely, this, bridgeConfig, (successful, authFailure, message1) -> {
            wasSuccessful = successful;
            wasAuthFailure = authFailure;
            //            if(countDownTimer != null) {
            //            countDownTimer.onFinish();
            //            }
            countDownTimer.onFinish();
        });
        countDownTimer = new CountDownTimer(60000, 3000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "Waiting for bridge to provision: " + (60000 - millisUntilFinished) + "/60000");
            }

            @Override
            public void onFinish() {
                if (wasSuccessful) {
                    Log.d(TAG, "Bridge provisioned!");
                    broadcastMessage("Bridge provisioned!");
                    updateState(DISCONNECTING);
                } else if (!wasAuthFailure && provisionSecurely) { // If failure wasn't auth related, try insecure provisioning
                    Log.d(TAG, "Secure provisioning failed, falling back to insecure");
                    updateState(PROVISIONING_INSECURE);
                } else {
                    Log.d(TAG, "Bridge provisioning failed.");
                    broadcastMessage("Bridge provisioning failed.");
                    updateState(DISCONNECTING);
                }
                if (serviceHandler != null) {
                    Message msg = serviceHandler.obtainMessage();
                    serviceHandler.handleMessage(msg);
                }
            }
        };
        countDownTimer.start();
    }

    /**
     * Disconnect from the bridge and remove it as a saved network
     */
    private void disconnectFromBridge() {
        Log.d(TAG, "Disconnecting from Bridge...");
        broadcastMessage("Disconnecting from Bridge...");
        wifiManager.removeNetwork(networkId);
        wifiManager.saveConfiguration();
        countDownTimer = new CountDownTimer(10000, 2000) {
            @Override
            public void onTick(long millisUntilFinished) {
                wifiManager.reconnect();
            }

            @Override
            public void onFinish() {
                int resultState;
                if (isConnectedToBridge()) {
                    Log.d(TAG, "Failed to disconnect from Bridge...");
                    broadcastMessage("Failed to disconnect from Bridge...");
                    resultState = BridgeConstants.Results.RESULT_CODE_DISCONNECTION_FAILURE;
                } else {
                    Log.d(TAG, "Disconnected from Bridge!");
                    broadcastMessage("Disconnected from Bridge!");
                    if (wasAuthFailure) {
                        resultState = BridgeConstants.Results.RESULT_CODE_CONNECTION_FAILURE_AUTH;
                    } else {
                        resultState = BridgeConstants.Results.RESULT_CODE_SUCCESS;
                    }
                }
                broadcastResult(wasSuccessful, resultState);
                stopSelf();
            }
        };
        countDownTimer.start();
    }

    private void registerConnectionReceiver() {
        if (connectionReceiver == null && !receiverRegistered.get()) {
            connectionReceiver = new ConnectionReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            registerReceiver(connectionReceiver, intentFilter);
            receiverRegistered.set(true);
        }
    }

    private void unregisterConnectionReceiver(ConnectionReceiver connectionReceiver) {
        if (receiverRegistered.get()) {
            unregisterReceiver(connectionReceiver);
            receiverRegistered.set(false);
        }
    }

    private int getBridgeNetworkIdFromWifiManager() {
        for (WifiConfiguration wifiConf : wifiManager.getConfiguredNetworks()) {
            if (wifiConf.SSID.equals("\"" + bridgeConfig.getBridgeSSID() + "\"")) {
                return wifiConf.networkId;
            }
        }
        return -1;
    }

    private boolean isConnectedToBridge() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            if (wifiInfo.getSSID().replace("\"", "").equals(bridgeConfig.getBridgeSSID())) {
                return true;
            }
        }
        return false;
    }

    private boolean isConnectedToWifi() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            if (wifiInfo.getSSID().equals(bridgeConfig.getNetworkSSID())) {
                Log.d("***DEBUG***", "isConnectedToBridge: connected to original wifi... disconnecting");
                return true;
            }
        }
        return false;
    }

    private void broadcastMessage(String message) {
        Intent localIntent = new Intent(MESSAGE_BROADCAST).putExtra(PROVISIONING_MESSAGE, message);
        LocalBroadcastManager.getInstance(BridgeProvisioningService.this).sendBroadcast(localIntent);
    }

    private void broadcastResult(boolean wasSuccessful, int resultCode) {
        Intent localIntent = new Intent(RESULT_BROADCAST)
                .putExtra(RESULT, wasSuccessful)
                .putExtra(RESULT_CODE, resultCode);
        LocalBroadcastManager.getInstance(BridgeProvisioningService.this).sendBroadcast(localIntent);
    }

    private void updateState(int newState) {
        if (CURRENT_STATE == newState) {
            Log.e(TAG, "WARNING: Attempting to change state to currently active state!");
        }

        Log.d(TAG, "Updating State Machine: " + getStateMachineStatus(CURRENT_STATE) + " -> " + getStateMachineStatus(newState));
        CURRENT_STATE = newState;
    }

    private String getStateMachineStatus(int state) {
        String stateString = "";
        switch (state) {
            case CONNECTING:
                stateString = "CONNECTING";
                break;
            case PROVISIONING_SECURE:
                stateString = "PROVISIONING_SECURE";
                break;
            case PROVISIONING_INSECURE:
                stateString = "PROVISIONING_INSECURE";
                break;
            case DISCONNECTING:
                stateString = "DISCONNECTING";
                break;
        }
        return stateString;
    }

    @Override
    public void updateFooterStatus(String status) {
        broadcastMessage(status);
    }

    @Override
    public void updateDialogStatus(boolean wasSuccessful, int resultCode) {
        Log.d(TAG, "updateDialogStatus: wasSuccessful: " + wasSuccessful + " resultcode: " + resultCode);
        // Use this for updating the the footer button in AddNewBridgeActivity via SecureProvisioner
        //        broadcastMessage("updateDialogStatus: wasSuccessful:" + wasSuccessful + " resultcode:" + resultCode);
    }
}
