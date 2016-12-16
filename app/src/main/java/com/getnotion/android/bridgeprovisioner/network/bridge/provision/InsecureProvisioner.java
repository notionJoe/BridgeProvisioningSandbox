package com.getnotion.android.bridgeprovisioner.network.bridge.provision;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.getnotion.android.bridgeprovisioner.network.bridge.provision.ProvisioningResult.AUTH_FAILURE;
import static com.getnotion.android.bridgeprovisioner.network.bridge.provision.ProvisioningResult.BRIDGE_PROVISIONED;
import static com.getnotion.android.bridgeprovisioner.network.bridge.provision.ProvisioningResult.INSECURE_PROVISIONING_FAILED;
import static com.getnotion.android.bridgeprovisioner.network.bridge.provision.ProvisioningResult.INSECURE_PROVISIONING_SUCCESS;
import static com.getnotion.android.bridgeprovisioner.network.bridge.provision.ProvisioningResult.NETWORK_NOT_FOUND;
import static com.getnotion.android.bridgeprovisioner.network.bridge.provision.ProvisioningResult.NOT_CONFIGURED;


public class InsecureProvisioner implements IBridgeProvisioner {

    private static final String TAG = "InsecureProvisioner";

    private OkHttpClient httpClient;

    public InsecureProvisioner(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Provision the device via the Marvell Java reference app functions -- this is much quicker,
     * and seems to work with most routers.
     * <p/>
     * Note that the wifi credentials are not encrypted before being sent to the bridge (via http
     * nonetheless). Use provisionBridge() to provision the device securely.
     */
    public void provision(BridgeConfig bridgeConfig, ProvisioningResult provisioningResult) {
        BridgeUtils.resetBridgeToProvisioningMode(httpClient);
        if(!checkBridgeStatus(provisioningResult)) {
            if (postSystemNetwork(bridgeConfig)) {
                clientAck();
                pollForBridgeStatus(provisioningResult);
            } else {
                provisioningResult.setResult(false, false, false, INSECURE_PROVISIONING_FAILED);
            }
        }
    }
    /**
     * Post network configuration to the bridge
     *
     * @return whether or not the post was successful
     */
    private boolean postSystemNetwork(BridgeConfig bridgeConfig) {
        Response response = null;
        try {
            Log.i(TAG, "Posting network credential to /sys/network/");

            // Create JSON body
            JSONObject jsonBody = new JSONObject();
            jsonBody.put(BridgeConstants.JsonParams.SSID, bridgeConfig.getNetworkSSID());
            jsonBody.put(BridgeConstants.JsonParams.SECURITY, Integer.valueOf(bridgeConfig.getNetworkSecurity()));
            jsonBody.put(BridgeConstants.JsonParams.CHANNEL, bridgeConfig.getNetworkChannel());
            jsonBody.put(BridgeConstants.JsonParams.PASSWORD, bridgeConfig.getNetworkPassword());
            jsonBody.put(BridgeConstants.JsonParams.IP, 1);

            RequestBody requestBody = RequestBody.create(BridgeConstants.getJsonRequestMediaType(), jsonBody.toString());
            Request request = new Request.Builder()
                    .url(BridgeConstants.Url.INSECURE_PROV)
                    .post(requestBody)
                    .build();
            response = httpClient.newCall(request).execute();

            if (response.isSuccessful()) {
                Log.i(TAG, "Successfully posted network credentials");
                Log.i(TAG, "\tResponse: " + response.body().string());
                return true;
            } else {
                Log.e(TAG, "Failed to post network credentials");
                Log.i(TAG, "\tResponse: " + response.body().string());
                throw new IOException("Unexpected response code " + response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Exception thrown in postSystemNetwork: " + e.getMessage());
            return false;
        } finally {
            if(response != null) {
                response.close();
            }
        }
    }

    /**
     * Check the current status of the bridge
     * <p/>
     * Response payload of /sys is as follows:
     * <p/>
     * {
     * {
     * "uuid": "xxxxxxxxxxxxxxxxxxxxxxxxxxxx",
     * "interface": "uap",
     * "prov": {
     * "types": []
     * },
     * "connection": {
     * "station": {
     * "mac_addr": "40-xx-xx-xx-xx-08",
     * "configured": 0,
     * "status": 0
     * },
     * "uap": {
     * "mac_addr": "40-xx-xx-xx-xx-08",
     * "ssid": "Notion Bridge: af0000fb",
     * "bssid": "40:e2:30:1d:e3:08",
     * "channel": 0,
     * "security": 0,
     * "iptype": 0
     * }
     * }
     * }
     * }
     *
     * @return boolean isConfigured
     */
    private boolean checkBridgeStatus(ProvisioningResult provisioningResult) {
        Response response = null;
        try {
            Log.i(TAG, "Getting bridge system status via /sys/");

            Request request = new Request.Builder()
                    .url(BridgeConstants.Url.SYS)
                    .get()
                    .build();
            response = httpClient.newCall(request).execute();


            String responseBodyString = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBodyString);
            Log.d(TAG, "Response: \n" + jsonResponse);

            if (response.isSuccessful()) {
                int configured = -1;
                int status = -1;

                //TODO: Refactor to parser with these keys abstracted out
                if (jsonResponse.has("connection")) {
                    JSONObject connectionObject = jsonResponse.getJSONObject("connection");
                    if (connectionObject.has("station")) {
                        JSONObject stationObject = connectionObject.getJSONObject("station");
                        configured = Integer.parseInt(stationObject.getString("configured"));
                        status = Integer.parseInt(stationObject.getString("status"));
                    }
                }

                if (configured != -1 && status != -1) {
                    if (configured == 0) {
                        Log.d(TAG, "checkBridgeStatus: not configured!");
                        provisioningResult.setResult(false, false, false, NOT_CONFIGURED);
                    } else if (configured == 1 && status == 2) {
                        provisioningResult.setResult(true, false, false, BRIDGE_PROVISIONED);
                        return true;
                    } else if (configured == 1) {
                        Log.d(TAG, "configured");
                        // Device not configured to network -- check for error cases
                        if (responseBodyString.contains("auth_failed")) {
                            Log.d(TAG, "\tauthentication failed");
                            provisioningResult.setResult(false, true, false, AUTH_FAILURE);
                            return true;
                        } else if (responseBodyString.contains("network_not_found")) {
                            Log.d(TAG, "\tnetwork not found");
                            provisioningResult.setResult(false, false, true, NETWORK_NOT_FOUND);
                            return true;
                        } else if (responseBodyString.contains("dhcp_failed")) {
                            Log.d(TAG, "\tDHCP Failure");
                        } else if (responseBodyString.contains("other")) {
                            Log.d(TAG, "\tother");
                        } else {
                            Log.d(TAG, "\tconnecting?");
                        }
                    }
                } else {
                    Log.e(TAG, "checkBridgeStatus response == successful but did not contain configured and/or status ");
                    Log.e(TAG, "\t response code: " + response.code());
                }
            } else {
                response.close();
                Log.e(TAG, "checkBridgeStatus: Request was unsuccessful!");
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        } finally {
            if(response != null) {
                response.close();
            }
        }
        return false;
    }
    private void pollForBridgeStatus(ProvisioningResult provisioningResult) {
        int attempts = 0;
        while (attempts < 4) {
            try {
                Thread.sleep(5000);
                if (checkBridgeStatus(provisioningResult)) {
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            attempts++;
        }
        if(provisioningResult.getMessage().equals(NOT_CONFIGURED)) {
            return;
        }
        provisioningResult.setResult(true, false, false, INSECURE_PROVISIONING_SUCCESS);
    }

    /**
     * Acknowledge provisioning success
     */
    private void clientAck() {
        Response response = null;
        try {
            Log.i(TAG, "Acknowledging successful provisioning");

            JSONObject jsonContent = new JSONObject();
            jsonContent.put("client_ack", 1);
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("prov", jsonContent);

            RequestBody requestBody = RequestBody.create(BridgeConstants.getUrlEncodedRequestMediaType(), jsonBody.toString());
            Request request = new Request.Builder()
                    .url(BridgeConstants.Url.SYS)
                    .post(requestBody)
                    .build();
            response = httpClient.newCall(request).execute();

            if (response.isSuccessful()) {
                Log.i(TAG, "Successfully acknowledged provisioning");
            } else {
                Log.e(TAG, "Failed to acknowledge provisioning");
                throw new IOException("Unexpected response code " + response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(response != null) {
                response.close();
            }
        }
    }

    //</editor-fold>
}
