package com.getnotion.android.bridgeprovisioner;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class NetworkUtils {

    public static String TAG = NetworkUtils.class.getSimpleName();

    public static final int SECURITY_NONE = 0;
    public static final int SECURITY_WEP = 1;
    public static final int SECURITY_PSK = 2;
    public static final int SECURITY_EAP = 3;


    /**
     * Get the Non5G ScanResult of the wifi network the device is currently connected to.
     *
     * @return ScanResult of the wifi network the device is currently connected to. Null if not
     * connected to any network.
     */
    public static ScanResult getCurrentWifiConnectionScanResult(Context ctx) {

        WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        ConnectivityManager connManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            return getCurrentNon5GWifiConnectionScanResult(wifiManager);
        }
        return null;
    }

    /**
     * Get the Non5G ScanResult of the wifi network the device is currently connected to.
     *
     * @param wifiManager The WifiManager to check the WifiInfo from
     * @return ScanResult of the wifi network the device is currently connected to. Null if not
     * connected to any network.
     */
    public static ScanResult getCurrentNon5GWifiConnectionScanResult(WifiManager wifiManager) {

        WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        String currentNon5GNetworkSSID = getConnectedNon5GNetworkSSID(wifiManager);

        if (connectionInfo != null && !connectionInfo.getSSID().isEmpty() && !connectionInfo.getSSID().equals("<unknown ssid>")) {

            for (ScanResult scanResult : wifiManager.getScanResults()) {
                if (scanResult.SSID.equals(currentNon5GNetworkSSID) && scanResult.frequency < 3000) {
                    return scanResult;
                }
            }
        }
        return null;
    }

    /**
     * Get the SSID of the non-5G variant of the network the device is currently connected to. If the
     * active wifi network is 5G, this will search for networks with the same name minus the common
     * '- 5G' postfix and verify it's frequency is not in the 5G band.
     * <p/>
     * //TODO: Investigate cleaner way for doing this. I recall seeing a non5G() method in some internal api, but don't know for sure
     *
     * @param wifiManager The WifiManager to check the WifiInfo from
     * @return String representation of the SSID
     */
    public static String getConnectedNon5GNetworkSSID(WifiManager wifiManager) {
        WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        String currentNetworkSSID = getConnectedNetworkSSID(wifiManager);
        String currentNon5GNetworkSSID = "";

        if (connectionInfo != null && !connectionInfo.getSSID().isEmpty() && !connectionInfo.getSSID().equals("<unknown ssid>")) {
            // getFrequency() is api 21+
            if (Build.VERSION.SDK_INT >= 21) {
                if (connectionInfo.getFrequency() >= 3000) {
                    if (currentNetworkSSID.endsWith(" - 5G")) {
                        currentNon5GNetworkSSID = currentNetworkSSID.substring(0, currentNetworkSSID.lastIndexOf(" - 5G"));
                    } else if (currentNetworkSSID.endsWith("-5G")) {
                        currentNon5GNetworkSSID = currentNetworkSSID.substring(0, currentNetworkSSID.lastIndexOf("-5G"));
                    } else if (currentNon5GNetworkSSID.endsWith("-5GHz")) { // Grab non-5G ssid name
                        currentNon5GNetworkSSID = currentNon5GNetworkSSID.substring(0, currentNon5GNetworkSSID.lastIndexOf("-5GHz"));
                    } else if (currentNon5GNetworkSSID.endsWith("-5G")) { // Grab non-5G ssid name
                        currentNon5GNetworkSSID = currentNon5GNetworkSSID.substring(0, currentNon5GNetworkSSID.lastIndexOf("-5G"));
                    }
                } else {
                    currentNon5GNetworkSSID = currentNetworkSSID;
                }
            }
        }

        return currentNon5GNetworkSSID;
    }

    /**
     * Remove 5G networks from the list of ScanResults provided
     *
     * @param scanResults List<ScanResult> to filter
     * @return List<ScanResult> without 5G networks
     */
    public static List<ScanResult> filterOut5GNetworks(List<ScanResult> scanResults) {
        List<ScanResult> filteredScanResults = new ArrayList<>();
        for (ScanResult scanResult : scanResults) {
            if (scanResult.frequency < 3000 && !TextUtils.isEmpty(scanResult.SSID)) {
                filteredScanResults.add(scanResult);
            }
        }
        return filteredScanResults;
    }

    /**
     * Remove networks with an emptystring SSID from the list of ScanResults provided
     *
     * @param scanResults List<ScanResult> to filter
     * @return List of ScanResults without emptystring SSID networks
     */
    public static List<ScanResult> filterOutEmptyNetworks(List<ScanResult> scanResults) {
        List<ScanResult> filteredScanResults = new ArrayList<>();
        for (ScanResult scanResult : scanResults) {
            if (!TextUtils.isEmpty(scanResult.SSID)) {
                filteredScanResults.add(scanResult);
            }
        }
        return filteredScanResults;
    }

    /**
     * Remove NotionBridge networks from the list of ScanResults provided
     *
     * @param scanResults List<ScanResult> to filter
     * @return List of ScanResults without NotionBridge networks
     */
    public static List<ScanResult> filterOutBridgeNetworks(List<ScanResult> scanResults) {
        List<ScanResult> filteredScanResults = new ArrayList<>();
        for (ScanResult scanResult : scanResults) {
            if (!scanResult.SSID.startsWith(AppData.BRIDGE_PREFIX)) {
                filteredScanResults.add(scanResult);
            }
        }
        return filteredScanResults;
    }

    /**
     * Get the SSID of the wifi network the device is currently connected to. If not connected to
     * any network or if the SSID is not broadcasted this will be empty.
     *
     * @param wifiManager The WifiManager to check the WifiInfo from
     * @return String representation of the SSID. Empty string if not connected to any network
     */
    public static String getConnectedNetworkSSID(WifiManager wifiManager) {
        WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        String currentNetworkSSID = "";

        if (connectionInfo != null && !connectionInfo.getSSID().isEmpty() && !connectionInfo.getSSID().equals("<unknown ssid>")) {
            currentNetworkSSID = connectionInfo.getSSID();

            // getSSID() return it w/ quotes, so strip those guys off. From docs:"If the SSID can be decoded as UTF-8,
            // it will be returned surrounded by double quotation marks. Otherwise, it is returned as a string of hex digits.
            // The SSID may be <unknown ssid> if there is no network currently connected."
            currentNetworkSSID = currentNetworkSSID.substring(1, currentNetworkSSID.length() - 1);
        }
        return currentNetworkSSID;
    }

    public static int getSecurityType(ScanResult scanResult) {
        if (scanResult.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (scanResult.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        } else if (scanResult.capabilities.contains("EAP")) {
            return SECURITY_EAP;
        }
        return SECURITY_NONE;
    }

    public static int getChannelFromFrequency(int frequency) {
        return Arrays.asList(0, 2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447, 2452, 2457, 2462, 2467, 2472, 2484).indexOf(frequency);
    }
}
