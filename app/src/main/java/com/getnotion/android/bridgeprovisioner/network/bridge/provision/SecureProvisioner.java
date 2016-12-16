package com.getnotion.android.bridgeprovisioner.network.bridge.provision;

import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.whispersystems.curve25519.Curve25519KeyPair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.DigestException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.ByteString;

import static com.getnotion.android.bridgeprovisioner.network.bridge.provision.ProvisioningResult.AUTH_FAILURE;
import static com.getnotion.android.bridgeprovisioner.network.bridge.provision.ProvisioningResult.SECURE_PROVISIONING_FAILED;
import static com.getnotion.android.bridgeprovisioner.network.bridge.provision.ProvisioningResult.SECURE_PROVISIONING_SUCCESS;

public class SecureProvisioner implements IBridgeProvisioner {

    private static final String TAG = "SecureProvisioner";
    private final OkHttpClient httpClient;
    private int checkConfiguredStatusRetryCount = 1;
    private final int CHECK_CONFIGURED_STATUS_RECOUNT = 5;
    private IProvisioningStatusListener provisioningStatusListener;

    public SecureProvisioner(OkHttpClient httpClient, IProvisioningStatusListener provisioningStatusListener) {
        this.httpClient = httpClient;
        this.provisioningStatusListener = provisioningStatusListener;
    }

    /**
     * Securely provision the device via the Marvell NodeJS reference app functions
     *
     * @throws Exception Something didn't quite go right...
     */
    @Override
    public void provision(final BridgeConfig bridgeConfig, ProvisioningResult provisioningResult) throws Exception {
        BridgeUtils.resetBridgeToProvisioningMode(httpClient);

        Curve25519Provider curve25519Provider = new Curve25519Provider();
        Curve25519KeyPair keyPair = curve25519Provider.getKeyPair();
        byte[] randomSignature = curve25519Provider.getRandomSignature(keyPair);

        JSONObject startSessionResponse = startSession(keyPair.getPublicKey(), randomSignature);
        String sessionId = startSessionResponse.getString(BridgeConstants.JsonParams.SESSION_ID);

        final byte[] sharedSecret = isSessionValid(curve25519Provider,
                                                   keyPair,
                                                   randomSignature,
                                                   startSessionResponse);

        SecretKeySpec secretKeySpec = new SecretKeySpec(sharedSecret, CryptoUtils.SECRET_KEY_ALGORITHM);

        if (sharedSecret != null) {
            final SecureSessionData secureSessionData = new SecureSessionData(bridgeConfig, secretKeySpec, sharedSecret, sessionId);

            JSONObject encryptedJson = sendNetworkCredentials(secureSessionData);
            JSONObject decryptedJson = getDecryptedJsonFromBridge(encryptedJson, secureSessionData);

            if (networkCredentialsAccepted(decryptedJson)) {
                while (checkConfiguredStatusRetryCount <= CHECK_CONFIGURED_STATUS_RECOUNT) {
                    Thread.sleep(5000);
                    Log.d(TAG, "Polling for BridgeConfiguredStatus: " + checkConfiguredStatusRetryCount + "/" + CHECK_CONFIGURED_STATUS_RECOUNT);
                    JSONObject bridgeStatus = getBridgeConfiguredStatus(secureSessionData);
                    if (bridgeStatus != null) {
                        if (checkBridgeStatus(bridgeStatus, provisioningResult)) {
                            sendAck(secureSessionData);
                            provisioningResult.setResult(true, false, false, SECURE_PROVISIONING_SUCCESS);
                            return;
                        } else if (provisioningResult.isAuthFailure()) {
                            provisioningStatusListener.updateDialogStatus(false, BridgeConstants.Results.RESULT_CODE_CONNECTION_FAILURE_AUTH);
                            provisioningResult.setResult(false, true, false, SECURE_PROVISIONING_FAILED);
                            return;
                        } else if(provisioningResult.isNetworkNotFound()) {
                            provisioningStatusListener.updateDialogStatus(false, BridgeConstants.Results.RESULT_CODE_CONNECTION_FAILURE_AUTH);
                            provisioningResult.setResult(false, false, true, SECURE_PROVISIONING_FAILED);
                            return;
                        }
                    }
                    checkConfiguredStatusRetryCount++;
                }
            } else {
                provisioningStatusListener.updateDialogStatus(false, BridgeConstants.Results.RESULT_CODE_CONNECTION_FAILURE_AUTH);
                provisioningResult.setResult(false, true, false, "Secure Provisioning failed");
                return;
            }
        }
        provisioningResult.setResult(false, false, false, "Secure Provisioning failed");
    }

    private JSONObject getBridgeConfiguredStatus(SecureSessionData secureSessionData) {
        try {
            Log.i(TAG, "Getting bridge system status via /sys/");

            Request request = new Request.Builder()
                    .url(BridgeConstants.Url.ACK_CHECK_STATUS + secureSessionData.getSessionId())
                    .get()
                    .build();
            Response response = httpClient.newCall(request).execute();

            if (response.isSuccessful()) {
                Log.i(TAG, "getBridgeConfiguredStatus: response successful");
                JSONObject jsonResponse = new JSONObject(response.body().string());
                response.close();
                return getDecryptedJsonFromBridge(jsonResponse, secureSessionData);
            }
            Log.i(TAG, "getBridgeConfiguredStatus: response not successful");

        } catch (Exception e) {
            Log.e(TAG, "getBridgeConfiguredStatus: " + e.getClass().toString() + " " + e.getMessage());
        }
        Log.d(TAG, "getBridgeConfiguredStatus: response was NOT successful...");
        return null;

    }

    private boolean checkBridgeStatus(JSONObject bridgeStatus, ProvisioningResult provisioningResult) {
        Log.d(TAG, "checking BridgeConfiguredStatus..." + bridgeStatus.toString());
        if (checkErrorMessage(bridgeStatus, provisioningResult)) {
            return false;
        }

        return checkBridgeConnection(bridgeStatus);
    }

    private boolean checkBridgeConnection(JSONObject bridgeStatus) {
        try {
            int status = bridgeStatus.getInt(BridgeConstants.JsonParams.STATUS);
            switch (status) {
                case BridgeConstants.ConnectionStatus.CONNECTED:
                    // We don't ever actually receive this message from the bridge...
                    Log.i(TAG, "Bridge Status: Connected!");
                    provisioningStatusListener.updateFooterStatus("Bridge connected!");
                    return true;
                case BridgeConstants.ConnectionStatus.CONNECTING:
                    Log.i(TAG, "Bridge Status: Connecting");
                    provisioningStatusListener.updateFooterStatus("Bridge is connecting...");
                    break;
                case BridgeConstants.ConnectionStatus.NOT_CONNECTED:
                    Log.i(TAG, "Bridge Status: Not Connected");
                    if(bridgeStatus.has("failure")) {
                        String error = bridgeStatus.getString("failure");
                        provisioningStatusListener.updateFooterStatus("Error: " + error);
                    }
                    break;
                default:
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean checkErrorMessage(JSONObject bridgeStatus, ProvisioningResult provisioningResult) {
        try {
            String error = bridgeStatus.getString("failure");
            switch (error) {
                case BridgeConstants.ErrorCodes.AUTH_FAILED:
                    provisioningResult.setMessage(AUTH_FAILURE);
                    provisioningResult.setAuthFailure(true);
                    break;
                case BridgeConstants.ErrorCodes.DHCP_FAILED:
                    provisioningResult.setMessage(BridgeConstants.ErrorCodes.DHCP_FAILED);
                    break;
                case BridgeConstants.ErrorCodes.NETWORK_NOT_FOUND:
                    provisioningResult.setMessage(BridgeConstants.ErrorCodes.NETWORK_NOT_FOUND);
                    provisioningResult.setNetworkNotFound(true);
                    break;
                case BridgeConstants.ErrorCodes.OTHER:
                    provisioningResult.setMessage(BridgeConstants.ErrorCodes.UNKNOWN_ERROR);
                    break;
            }
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    private byte[] isSessionValid(Curve25519Provider curve25519Provider, Curve25519KeyPair keyPair, byte[] randomSignature, JSONObject
            startSessionResponse) throws
            JSONException, NoSuchAlgorithmException, DigestException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, UnsupportedEncodingException {
        if (validateResponseChecksum(startSessionResponse, randomSignature)) {
            String publicKey = startSessionResponse.getString(BridgeConstants.JsonParams.DEVICE_PUB_KEY);
            byte[] publicKeyBytes = ByteString.decodeHex(publicKey).toByteArray();
            byte[] privateKeyBytes = keyPair.getPrivateKey();

            byte[] curve25519Secret = curve25519Provider.getCurveSharedSecret(publicKeyBytes, privateKeyBytes);
            byte[] sharedSecret = deriveSharedSecret(curve25519Secret);

            byte[] decryptedStartSessionData = getDecryptedBytesFromBridge(startSessionResponse, sharedSecret);
            if (Arrays.equals(decryptedStartSessionData, randomSignature)) {
                return sharedSecret;
            }
        }
        return null;
    }

    private JSONObject sendNetworkCredentials(SecureSessionData secureSessionData) throws
            JSONException, DigestException, NoSuchAlgorithmException, IOException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, InterruptedException {
        JSONObject jsonNetworkCredentials = parseJsonFromBridgeConfig(secureSessionData);

        byte[] decryptedNetworkCredPayload = (jsonNetworkCredentials.toString()).getBytes(CryptoUtils.ENCODING);
        byte[] networkCredSha512 = CryptoUtils.getSha512Checksum(decryptedNetworkCredPayload);
        byte[] aesPaddedPayload = CryptoUtils.padPayload(decryptedNetworkCredPayload);

        byte[] randomIv = CryptoUtils.generateRandomIv();
        IvParameterSpec encryptIvParameterSpec = new IvParameterSpec(randomIv);

        Cipher encryptCipher = Cipher.getInstance(CryptoUtils.CIPHER_TRANSFORMATION);
        encryptCipher.init(Cipher.ENCRYPT_MODE, secureSessionData.getSecretKeySpec(), encryptIvParameterSpec);
        byte[] encryptedDataBytes = encryptCipher.doFinal(aesPaddedPayload);

        JSONObject jsonConfigureNetwork = new JSONObject();
        jsonConfigureNetwork.put(BridgeConstants.JsonParams.IV, CryptoUtils.convertBytesToHex(randomIv));
        jsonConfigureNetwork.put(BridgeConstants.JsonParams.DATA, CryptoUtils.convertBytesToHex(encryptedDataBytes));
        jsonConfigureNetwork.put(BridgeConstants.JsonParams.CHECKSUM, CryptoUtils.convertBytesToHex(networkCredSha512));

        RequestBody requestBody = RequestBody
                .create(BridgeConstants.getUrlEncodedRequestMediaType(), jsonConfigureNetwork.toString() + CryptoUtils.NULL_TERMINATOR);
        Request request = new Request.Builder()
                .url(BridgeConstants.Url.SECURE_PROV + secureSessionData.getSessionId())
                .post(requestBody)
                .build();

        Response response = httpClient.newCall(request).execute();
        JSONObject responseJSON = new JSONObject(response.body().string());
        response.close();
        return responseJSON;

    }

    @NonNull
    private JSONObject parseJsonFromBridgeConfig(SecureSessionData secureSessionData) throws JSONException {
        String ssid = secureSessionData.getBridgeConfig().getNetworkSSID();
        String security = secureSessionData.getBridgeConfig().getNetworkSecurity();
        String password = secureSessionData.getBridgeConfig().getNetworkPassword();

        JSONObject jsonNetworkCredentials = new JSONObject();
        jsonNetworkCredentials.put(BridgeConstants.JsonParams.SSID, ssid);
        jsonNetworkCredentials.put(BridgeConstants.JsonParams.SECURITY, Integer.valueOf(security));
        jsonNetworkCredentials.put(BridgeConstants.JsonParams.PASSWORD, password);
        return jsonNetworkCredentials;
    }

    private boolean validateResponseChecksum(JSONObject startSessionResponse, byte[] randomSignature) {
        try {
            String checksum  = startSessionResponse.getString(BridgeConstants.JsonParams.CHECKSUM);
            return CryptoUtils.isChecksumValid(checksum, randomSignature);
        } catch (NoSuchAlgorithmException | JSONException | DigestException e) {
            e.printStackTrace();
            return false;
        }
    }

    private JSONObject getDecryptedJsonFromBridge(JSONObject response, SecureSessionData secureSessionData) throws
            JSONException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, UnsupportedEncodingException {
        byte[] decryptedResponseBytes = getDecryptedBytesFromBridge(response, secureSessionData);

        String actualResponse = new String(decryptedResponseBytes, CryptoUtils.ENCODING);
        return new JSONObject(actualResponse);
    }

    private byte[] getDecryptedBytesFromBridge(JSONObject response, SecureSessionData secureSessionData) throws
            JSONException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        String iv = response.getString(BridgeConstants.JsonParams.IV);
        String data = response.getString(BridgeConstants.JsonParams.DATA);
        // TODO: Verify checksum...
        String checksum = response.getString(BridgeConstants.JsonParams.CHECKSUM);


        IvParameterSpec responseIvSpec = new IvParameterSpec(ByteString.decodeHex(iv).toByteArray());

        Cipher decryptCipher = Cipher.getInstance(CryptoUtils.CIPHER_TRANSFORMATION);
        decryptCipher.init(Cipher.DECRYPT_MODE, secureSessionData.getSecretKeySpec(), responseIvSpec);

        return decryptCipher.doFinal(ByteString.decodeHex(data).toByteArray());
    }

    private byte[] getDecryptedBytesFromBridge(JSONObject response, byte[] sharedSecret) throws
            JSONException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        String iv = response.getString(BridgeConstants.JsonParams.IV);
        String data = response.getString(BridgeConstants.JsonParams.DATA);
        // TODO: Verify checksum...
        String checksum = response.getString(BridgeConstants.JsonParams.CHECKSUM);

        IvParameterSpec responseIvSpec = new IvParameterSpec(ByteString.decodeHex(iv).toByteArray());
        SecretKeySpec secretKeySpec = new SecretKeySpec(sharedSecret, CryptoUtils.SECRET_KEY_ALGORITHM);

        Cipher decryptCipher = Cipher.getInstance(CryptoUtils.CIPHER_TRANSFORMATION);
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, responseIvSpec);

        return decryptCipher.doFinal(ByteString.decodeHex(data).toByteArray());
    }

    private byte[] deriveSharedSecret(byte[] curveSharedSecret) throws NoSuchAlgorithmException, DigestException {
        byte[] sharedSecretXSha512 = CryptoUtils.getSha512Checksum(curveSharedSecret);
        byte[] provisioningPinSha512 = CryptoUtils.getSha512Checksum(BridgeConstants.MetaData.PROVISIONING_PIN.getBytes());

        byte[] sharedSecret = new byte[16];

        for (int i = 0; i < 16; i++) {
            sharedSecret[i] = ((byte) (sharedSecretXSha512[i] ^ provisioningPinSha512[i]));
        }
        return sharedSecret;
    }

    /**
     * Starts a session with the bridge by POSTing the client public key and signature to '/prov/secure-session'
     *
     * @param publicKey The publicKey generated as specified in the Marvell documentation
     * @param randomSig The randomSig generated as specified in the Marvell documentation
     * @return JSONObject Response of the request to start a session
     * @throws IOException
     * @throws JSONException
     */
    private JSONObject startSession(byte[] publicKey, byte[] randomSig) throws IOException, JSONException {
        Log.i(TAG,
              "Attempting to start session with bridge...");

        // Create JSON body
        String publicKeyHex = CryptoUtils.convertBytesToHex(publicKey);
        String randomSigHex = CryptoUtils.convertBytesToHex(randomSig);

        JSONObject jsonBody = new JSONObject();
        jsonBody.put(BridgeConstants.JsonParams.CLIENT_PUB_KEY, publicKeyHex);
        jsonBody.put(BridgeConstants.JsonParams.RANDOM_SIGNATURE, randomSigHex);

        RequestBody requestBody = RequestBody.create(BridgeConstants.getUrlEncodedRequestMediaType(), jsonBody.toString());

        Request request = new Request.Builder()
                .url(BridgeConstants.Url.START_SECURE_SESSION)
                .post(requestBody)
                .build();

        Response response = httpClient.newCall(request).execute();
        // If not successful, throw up the error
        if (!response.isSuccessful()) {
            Log.e(TAG, "Failed to start session with bridge");
            throw new IOException("Unexpected response code " + response);
        }

        // Extract device_pub_key, iv (init vector), data, checksum & session_id
        JSONObject jsonObject = new JSONObject(response.body().string());
        response.close();

        Log.i(TAG, "Started session with bridge!");
        return jsonObject;
    }

    private JSONObject sendAck(SecureSessionData secureSessionData) throws Exception {
        Log.i(TAG, "Sending ack of provisioning complete...");

        byte[] newRandomIv = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(newRandomIv);

        IvParameterSpec newEncryptIvParameterSpec = new IvParameterSpec(newRandomIv);

        Cipher ackEncryptCipher = Cipher.getInstance(CryptoUtils.CIPHER_TRANSFORMATION);
        ackEncryptCipher.init(Cipher.ENCRYPT_MODE, secureSessionData.getSecretKeySpec(), newEncryptIvParameterSpec);

        JSONObject ackData = new JSONObject();
        ackData.put(BridgeConstants.JsonParams.SECURE_ACK, 1);
        byte[] decryptedAckPayload = (ackData.toString()).getBytes(CryptoUtils.ENCODING);
        byte[] ackChecksum = CryptoUtils.getSha512Checksum(decryptedAckPayload);
        byte[] aesPaddedPayload = CryptoUtils.padPayload(decryptedAckPayload);

        Cipher encryptCipher = Cipher.getInstance(CryptoUtils.CIPHER_TRANSFORMATION);
        encryptCipher.init(Cipher.ENCRYPT_MODE,
                           secureSessionData.getSecretKeySpec(),
                           newEncryptIvParameterSpec);
        byte[] encryptedDataBytes = encryptCipher.doFinal(aesPaddedPayload);

        JSONObject jsonAck = new JSONObject();
        jsonAck.put(BridgeConstants.JsonParams.IV, CryptoUtils.convertBytesToHex(newRandomIv));
        jsonAck.put(BridgeConstants.JsonParams.DATA, CryptoUtils.convertBytesToHex(encryptedDataBytes));
        jsonAck.put(BridgeConstants.JsonParams.CHECKSUM, CryptoUtils.convertBytesToHex(ackChecksum));

        RequestBody requestBody = RequestBody.create(BridgeConstants.getUrlEncodedRequestMediaType(),
                                                     jsonAck.toString() + CryptoUtils.NULL_TERMINATOR);

        Request request = new Request.Builder()
                .url(BridgeConstants.Url.ACK_CHECK_STATUS + secureSessionData.getSessionId())
                .post(requestBody)
                .build();

        Response response = httpClient.newCall(request).execute();

        // If not successful, throw up the error
        if (!response.isSuccessful()) {
            Log.e(TAG, "Failed to acknowledge provisioning complete!");
            Log.e(TAG, "EntityResponse: " + response.body().toString());
            response.close();
            throw new IOException("Unexpected response code " + response);
        }

        JSONObject responseJSON = new JSONObject(response.body().string());
        response.close();
        return  responseJSON;
    }

    private boolean networkCredentialsAccepted(JSONObject decryptedNetworkCredentialResponse) throws JSONException {

        // We will almost certainly not get these error codes from the bridge at this point.
        // The error codes are not checking if the ACTUAL network credentials are good, but
        // rather, if the format/padding/parameters of the message are what the bridge expects

        if (decryptedNetworkCredentialResponse.has(BridgeConstants.JsonParams.SUCCESS)) {
            System.out.println("CheckNetworkCredentialsResponse: Bridge successfully received network credentials");
            return true;

        }

        return parseErrorCodeFromBridge(decryptedNetworkCredentialResponse);
    }

    private boolean parseErrorCodeFromBridge(JSONObject decryptedNetworkCredentialResponse) throws JSONException {
        Log.d(TAG, "parseErrorCodeFromBridge: checking bridge response for error...");
        if (decryptedNetworkCredentialResponse.has(BridgeConstants.JsonParams.ERROR_CODE)) {
            int errorCode = Integer.valueOf(decryptedNetworkCredentialResponse.getString(BridgeConstants.JsonParams.ERROR_CODE));
            String message = "NetworkCredentialsResponse contains error ";
            switch (errorCode) {
                case BridgeConstants.ErrorCodes.INVALID_SESSION_KEY:
                    Log.e(TAG, message + BridgeConstants.ErrorCodes.INVALID_SESSION);
                    break;
                case BridgeConstants.ErrorCodes.OUT_OF_MEMORY_KEY:
                    Log.e(TAG, message + BridgeConstants.ErrorCodes.OUT_OF_MEMORY);
                    break;
                case BridgeConstants.ErrorCodes.INVALID_PARAMETERS_KEY:
                    Log.e(TAG, message + BridgeConstants.ErrorCodes.INVALID_PARAMETERS);
                    break;
                case BridgeConstants.ErrorCodes.INTERNAL_ERROR_KEY:
                    Log.e(TAG, message + BridgeConstants.ErrorCodes.INTERNAL_ERROR);
                    break;
                default:
                    Log.e(TAG, message + BridgeConstants.ErrorCodes.UNKNOWN_ERROR);
                    break;
            }
            return false;
        }
        Log.d(TAG, "NetworkCredentialsResponse contains NO ERRORS!!!!");
        return true;
    }
}
