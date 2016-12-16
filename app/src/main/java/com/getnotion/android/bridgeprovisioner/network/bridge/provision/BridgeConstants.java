package com.getnotion.android.bridgeprovisioner.network.bridge.provision;

import okhttp3.MediaType;

import static com.getnotion.android.bridgeprovisioner.network.bridge.provision.BridgeConstants.MetaData.REQUEST_MEDIA_TYPE_JSON;
import static com.getnotion.android.bridgeprovisioner.network.bridge.provision.BridgeConstants.MetaData.REQUEST_MEDIA_TYPE_URL_ENCODED;


public class BridgeConstants {

    public static MediaType getUrlEncodedRequestMediaType() {
        return MediaType.parse(REQUEST_MEDIA_TYPE_URL_ENCODED);
    }

    public static MediaType getJsonRequestMediaType() {
        return MediaType.parse(REQUEST_MEDIA_TYPE_JSON);
    }

    public class MetaData {
        public static final String PROVISIONING_PIN = "12345678";
        public static final String REQUEST_MEDIA_TYPE_URL_ENCODED = "application/x-www-form-urlencoded";
        public static final String REQUEST_MEDIA_TYPE_JSON = "application/json";
        public static final int CONNECTION_TIMEOUT = 15;
        public static final int READ_TIMEOUT = 10;
        public static final int WRITE_TIMEOUT = 0;
    }

    public class Url {
        public static final String BASE_URL = "http://192.168.10.1";
        public static final String SYS = BASE_URL + "/sys/";
        public static final String INSECURE_PROV = BASE_URL + "/sys/network";
        public static final String SECURE_PROV = BASE_URL + "/prov/network?session_id=";
        public static final String START_SECURE_SESSION = BASE_URL + "/prov/secure-session";
        public static final String ACK_CHECK_STATUS = BASE_URL + "/prov/net-info?session_id=";
    }

    public class JsonParams {
        public static final String SESSION_ID = "session_id";
        public static final String CONFIGURED = "configured";
        public static final String CHANNEL = "channel";
        public static final String SSID = "ssid";
        public static final String SECURITY = "security";
        public static final String PASSWORD = "key";
        public static final String IV = "iv";
        public static final String DATA = "data";
        public static final String CHECKSUM = "checksum";
        public static final String SUCCESS = "success";
        public static final String ERROR_CODE = "error-code";
        public static final String DEVICE_PUB_KEY = "device_pub_key";
        public static final String CLIENT_PUB_KEY = "client_pub_key";
        public static final String RANDOM_SIGNATURE = "random_sig";
        public static final String STATUS = "status";
        public static final String IP = "ip";
        public static final String SECURE_ACK = "prov_client_ack";
        public static final String SECURE_SESSION_STATUS = "secure-session-status";
        public static final String STATION = "station";
        public static final String CONNECTION = "connection";

    }

    public class ConnectionStatus {
        public static final int NOT_CONNECTED = 0;
        public static final int CONNECTING = 1;
        public static final int CONNECTED = 2;
    }

    public class ErrorCodes {
        public static final String INVALID_SESSION = "Invalid Session";
        public static final String OUT_OF_MEMORY = "Out of Memory";
        public static final String INVALID_PARAMETERS = "Invalid Parameters";
        public static final String INTERNAL_ERROR = "Internal Error";
        public static final String UNKNOWN_ERROR = "Unknown error.";

        public static final int INVALID_SESSION_KEY = -1;
        public static final int OUT_OF_MEMORY_KEY = -2;
        public static final int INVALID_PARAMETERS_KEY = -3;
        public static final int INTERNAL_ERROR_KEY = -4;

        public static final String AUTH_FAILED = "auth_failed";
        public static final String NETWORK_NOT_FOUND = "network_not_found";
        public static final String DHCP_FAILED = "dhcp_failed";
        public static final String OTHER = "other";
    }

    public class Results {
        public static final int RESULT_CODE_SUCCESS = 0;
        public static final int RESULT_CODE_FAILURE = 1;
        public static final int RESULT_CODE_CONNECTION_FAILURE = 2;
        public static final int RESULT_CODE_DISCONNECTION_FAILURE = 3;
        public static final int RESULT_CODE_CONNECTION_FAILURE_AUTH = 4;
    }
}
