package com.getnotion.android.bridgeprovisioner.network.bridge.provision;

import android.content.Context;
import android.getnotion.android.bridgeprovisioner.R;
import android.util.Log;


import com.getnotion.android.bridgeprovisioner.models.NotionBridge;
import com.getnotion.android.bridgeprovisioner.network.RestClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit.RetrofitError;

public class BridgeUtils {

    private static final String TAG = BridgeUtils.class.getSimpleName();


    private static final int SHORT_HARDWARE_ID_LENGTH = 8;


    /**
     * Reset the bridge into provisioning mode
     *
     * @return whether or not the bridge has been reset
     */
    public static void resetBridgeToProvisioningMode(OkHttpClient httpClient) {
        if (existingSecureSession(httpClient)) {
            clearExistingSecureSession(httpClient);
        }
    }

    private static void clearExistingSecureSession(OkHttpClient httpClient) {
        try {
            Log.d(TAG, "There is an existing Secure Session. Resetting Bridge to Provisioning Mode");
            sendResetBridgeMessage(httpClient);
        } catch (Exception e) {
            sendResetBridgeMessage(httpClient);
        }
    }

    private static boolean sendResetBridgeMessage(OkHttpClient httpClient) {
        Response response = null;
        try {
            Log.d(TAG, "Reset to bridge to provisioning mode");
            JSONObject jsonConfiguredObject = new JSONObject();
            jsonConfiguredObject.put(BridgeConstants.JsonParams.CONFIGURED, 0);

            JSONObject jsonStationObject = new JSONObject();
            jsonStationObject.put(BridgeConstants.JsonParams.STATION, jsonConfiguredObject);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put(BridgeConstants.JsonParams.CONNECTION, jsonStationObject);

            RequestBody requestBody = RequestBody.create(BridgeConstants.getJsonRequestMediaType(), jsonBody.toString());
            Request request = new Request.Builder()
                    .url(BridgeConstants.Url.SYS)
                    .post(requestBody)
                    .build();
            response = httpClient.newCall(request).execute();

            // If not successful, throw up the error
            if (response.isSuccessful()) {
                Log.i(TAG, "Successfully reset the bride into provisioning mode!");
                response.close();
                return true;
            } else {
                Log.e(TAG, "Failed reset the bride into provisioning mode");
                throw new IOException("Unexpected response code " + response.code());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(response != null) {
                response.body().close();
            }
        }
        return false;
    }

    /**
     * Returns whether or not an existing secure session exists.
     * <p/>
     * Performs an HTTP GET to '/prov/secure-session'
     */
    private static boolean existingSecureSession(OkHttpClient httpClient) {
        boolean secureSessionExists = false;
        Response response = null;
        try {

            Log.i(TAG, "Checking if a secure session already exists...");

            Request request = new Request.Builder()
                    .url(BridgeConstants.Url.START_SECURE_SESSION)
                    .build();
            response = httpClient.newCall(request).execute();

            // If not successful, throw up the error
            if (!response.isSuccessful()) {
                Log.e(TAG, "Failed to check if secure session exists!");
                Log.e(TAG, "Response: " + response.body().toString());
                response.close();
                throw new IOException("Unexpected response code " + response);
            }

            JSONObject jsonObject = new JSONObject(response.body().string());
            if (jsonObject.has(BridgeConstants.JsonParams.SECURE_SESSION_STATUS)) {
                secureSessionExists = Boolean.valueOf(jsonObject.getString(BridgeConstants.JsonParams.SECURE_SESSION_STATUS));
            }

            Log.i(TAG, "Existing secure session: " + secureSessionExists);
        } catch (JSONException e) {
            Log.d(TAG, "existingSecureSession: threw JSON exception");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "existingSecureSession: threw IO exception");
            e.printStackTrace();
        } finally {
            if(response != null) {
                response.body().close();
            }
        }
        return secureSessionExists;
    }

    /**
     * Gets the appropriate user facing error message for the response code returned when attempting
     * to add a bridge resource.
     *
     * @param ctx           Current context for fetching the string resource
     * @param retrofitError Error provided by retrofit for looking at kind and response code
     * @return User facing error string
     */
    public static String getErrorMessageForResponse(Context ctx, RetrofitError retrofitError) {

        String error = ctx.getString(R.string.dialogs_addingBridgeError_message);

        if (retrofitError.getKind() == RetrofitError.Kind.NETWORK) {
            error = ctx.getString(R.string.errors_bridge_responseCodeErrors_networkNotAvailable);
        } else if (retrofitError.getKind() == RetrofitError.Kind.HTTP && retrofitError.getResponse() != null) {
            error = RestClient.parseFailedRequest(retrofitError);
        }

        return error;
    }
}
