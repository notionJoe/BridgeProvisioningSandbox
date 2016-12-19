/*
  * Copyright (c) 2015 Loop Labs, Inc. All rights reserved.
 */

package com.getnotion.android.bridgeprovisioner.network.bridge.provision;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;


/**
 * Singleton for bridge (AW-CU288) provisioning to a non-5G wireless network. This provisioning
 * method is based on the nodeJS reference app as provided by Marvell (as of 4/27/15) located here:
 * <p/>
 * ownCloud/Hardware/Projects/NotionBase/Doc/Marvell_SDK/Homekit_beta_12/hap_mc200_sdk_bundle-0.9.246/hap_sdk/mext/mext_prov/prov-client-reference-app
 * <p/>
 * Provisioning Steps:
 * 1. Check Existing Secure Session
 * 2. Setup Secure Session
 * 3. Configure Network
 * 4. Check Accessory's Status
 * 5. Confirm Provisioning Completion
 * 6. Execute provided callback
 **/

//TODO Redo Java Docs + Update Comments Throughout...
public class BridgeProvisioner {

    private static final String TAG = BridgeProvisioner.class.getName();

    private OkHttpClient.Builder httpClientBuilder;
    private OkHttpClient httpClient;

    private AsyncTask<String, Void, Boolean> provisioningAsyncTask;

    private static BridgeProvisioner ourInstance = new BridgeProvisioner();

    private BridgeProvisioner() {
        initializeHttpClient();
    }

    public static BridgeProvisioner getInstance() {
        return ourInstance;
    }

    private void initializeHttpClient() {
        httpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(BridgeConstants.MetaData.CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(BridgeConstants.MetaData.READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(BridgeConstants.MetaData.WRITE_TIMEOUT, TimeUnit.SECONDS);
    }

    public void tearDown() {
        provisioningAsyncTask = null;
    }

    /**
     * Provision a bridge with the supplied credentials
     * NOTE: You must already be connected and maintain an active connection to the bridge throughout the provisioning process!
     *
     * @param activityContext       Context used to retrieve the WifiManager to ensure we're connected to the right device, also used for opening the system file containing the Marvell code
     * @param useSecureProvisioning
     * @param bridgeConfig          Config
     * @param callback              Callback called once the bridge has been successfully provisioned, an error has occured, or the process has timed out.
     */
    public void provisionBridge(final Context activityContext, boolean useSecureProvisioning,
                                final IProvisioningStatusListener provisioningStatusListener,
                                final BridgeConfig bridgeConfig, final OnBridgeProvisionedCallback callback) {
        final ProvisioningResult provisioningResult = new ProvisioningResult();

        bindHttpClientToCustomSocket(activityContext, bridgeConfig);

        this.httpClient = httpClientBuilder.build();

        if(provisioningAsyncTask != null) {
            provisioningAsyncTask.cancel(true);
        }
        // Throw this in the background
        // Ensure we're connected to the Bridge in question...
        // Make sure we're back on the UI thread for the callback
        if (useSecureProvisioning) {
            provisioningAsyncTask = getSecureProvisioningTask(activityContext, bridgeConfig, provisioningResult, provisioningStatusListener,
                                                              callback);
        } else {
            provisioningAsyncTask = getInsecureProvisioningTask(activityContext, bridgeConfig, provisioningResult,
                                                                provisioningStatusListener, callback);
        }


        // JellyBean doesn't care for straight execute(), must specify the executor
        provisioningAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void bindHttpClientToCustomSocket(Context activityContext, BridgeConfig bridgeConfig) {
        WifiManager wm = (WifiManager) activityContext.getSystemService(Context.WIFI_SERVICE);
        ConnectivityManager cm = (ConnectivityManager) activityContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        // If on lollipop+, bind to bridge network before provisioning
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            // Grab all available networks
            Network[] networks = cm.getAllNetworks();

            // Grab networkId of bridge
            int networkId = -1;
            for (WifiConfiguration wifiConf : wm.getConfiguredNetworks()) {
                if (wifiConf.SSID.equals("\"" + bridgeConfig.getBridgeSSID() + "\"")) {
                    networkId = wifiConf.networkId;
                }
            }

            // Bind our http client to a custom socket
            // TODO: This doesn't seem to be binding in most cases. Verify Samsung devices still don't
            // need this (especially on 5.0), and yank if so
            for (Network network : networks) {
                if (network.toString().equals(String.valueOf(networkId))) {
                    setHttpClientSocketFactory(network);
                }
            }
        }
    }


    private void setHttpClientSocketFactory(Network network) {
        httpClientBuilder.socketFactory(new BoundSocketFactory(network));
    }

    private AsyncTask<String, Void, Boolean> getSecureProvisioningTask(Context activityContext,
                                                                       BridgeConfig bridgeConfig,
                                                                       ProvisioningResult provisioningResult,
                                                                       IProvisioningStatusListener provisioningStatusListener,
                                                                       OnBridgeProvisionedCallback callback) {
        return new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... params) {
                // Ensure we're connected to the Bridge in question...
                if (isConnectedToBridge(activityContext, bridgeConfig, provisioningResult, provisioningStatusListener)) {
                    try {
                        new SecureProvisioner(httpClient, provisioningStatusListener).provision(bridgeConfig, provisioningResult);

                        if (provisioningResult.isCompleted()) {
                            return null;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Provisioning failed", e);
                        provisioningResult.setResult(false, false, false, "Secure provisioning failed with exception " + e.getMessage());
                        provisioningStatusListener.updateDialogStatus(false, BridgeConstants.Results.RESULT_CODE_FAILURE);
                    }

                }
                return null;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                // Make sure we're back on the UI thread for the callback
                callback.onBridgeProvisioned(provisioningResult.isCompleted(),
                                             provisioningResult.isAuthFailure(),
                                             provisioningResult.getMessage());
            }
        };
        // JellyBean doesn't care for straight execute(), must specify the executor
    }

    private AsyncTask<String, Void, Boolean> getInsecureProvisioningTask(Context activityContext,
                                                                         BridgeConfig bridgeConfig,
                                                                         ProvisioningResult provisioningResult,
                                                                         IProvisioningStatusListener provisioningStatusListener,
                                                                         OnBridgeProvisionedCallback callback) {
        return new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... params) {
                if (isConnectedToBridge(activityContext, bridgeConfig, provisioningResult, provisioningStatusListener)) {
                    try {
                        new InsecureProvisioner(httpClient).provision(bridgeConfig, provisioningResult);

                        if (provisioningResult.isCompleted()) {
                            return null;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Provisioning failed", e);
                        provisioningResult.setResult(false, false, false, "Provisioning failed with exception " + e.getMessage());
                        provisioningStatusListener.updateDialogStatus(false, BridgeConstants.Results.RESULT_CODE_FAILURE);
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                // Make sure we're back on the UI thread for the callback
                callback.onBridgeProvisioned(provisioningResult.isCompleted(),
                                             provisioningResult.isAuthFailure(),
                                             provisioningResult.getMessage());
            }
        };
    }

    private boolean isConnectedToBridge(Context activityContext, BridgeConfig bridgeConfig, ProvisioningResult provisioningResult, IProvisioningStatusListener provisioningStatusListener) {
        WifiManager wifiManager = (WifiManager) activityContext.getSystemService(Context.WIFI_SERVICE);
        if (bridgeConfig.getBridgeSSID() != null) {
            Log.d(TAG, "isConnectedToBridge: connectedWifi= " + wifiManager.getConnectionInfo().getSSID());
            if (!wifiManager.getConnectionInfo().getSSID().equals("\"" + bridgeConfig.getBridgeSSID() + "\"")) {
                provisioningResult.setResult(false, false, false, "Phone failed to connect to bridge access point");
                provisioningStatusListener.updateDialogStatus(false, BridgeConstants.Results.RESULT_CODE_CONNECTION_FAILURE_AUTH);
                Log.d(TAG, "isConnectedToBridge: false");
                return false;
            }
        } else {
            Log.d(TAG, "isConnectedToBridge: bridgeConfig.getBridgeSSID == null!!!!");
        }
        Log.d(TAG, "isConnectedToBridge: true");
        return true;
    }
}