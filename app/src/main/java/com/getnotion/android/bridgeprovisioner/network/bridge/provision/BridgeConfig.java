package com.getnotion.android.bridgeprovisioner.network.bridge.provision;

import com.getnotion.android.bridgeprovisioner.NetworkUtils;
import android.net.wifi.ScanResult;
import android.os.Parcel;
import android.os.Parcelable;


/**
 * Config object to hold all data needed to provision a bridge
 * <p/>
 * bridgeSSID       The bridge in question to be provisioned
 * networkSSID      networkSSID of the network to provision the bridge to
 * networkSecurity  Security type of the network to provision the bridge to. 0 - No Security, 1 - WEP, 4 - WPA2, 5 - WPA/WPA2 Mixed
 * networkPassword         Password of the network to provision to. If no networkSecurity (0), value is ignored (please provide empty string)
 * networkChannel          Channel from wifi frequency via NetworkUtils.getChannelFromFrequency(systemNetworkField.getSelectedScanResult().frequency);
 */
public class BridgeConfig implements Parcelable {

    private String bridgeSSID;
    private String networkSSID;
    private String networkSecurity;
    private String networkPassword;
    private int networkChannel;

    public BridgeConfig() {
    }

    public String getBridgeSSID() {
        return bridgeSSID;
    }

    public void setBridgeSSID(String bridgeSSID) {
        this.bridgeSSID = bridgeSSID;
    }

    public String getNetworkSSID() {
        return networkSSID;
    }

    public void setNetworkSSID(String networkSSID) {
        this.networkSSID = networkSSID;
    }

    public String getNetworkSecurity() {
        return networkSecurity;
    }

    public void setNetworkSecurity(String networkSecurity) {
        this.networkSecurity = networkSecurity;
    }

    public String getNetworkPassword() {
        return networkPassword;
    }

    public void setNetworkPassword(String networkPassword) {
        this.networkPassword = networkPassword;
    }

    public int getNetworkChannel() {
        return networkChannel;
    }

    public void setNetworkChannel(int networkChannel) {
        this.networkChannel = networkChannel;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.bridgeSSID);
        dest.writeString(this.networkSSID);
        dest.writeString(this.networkSecurity);
        dest.writeString(this.networkPassword);
        dest.writeInt(this.networkChannel);
    }

    protected BridgeConfig(Parcel in) {
        this.bridgeSSID = in.readString();
        this.networkSSID = in.readString();
        this.networkSecurity = in.readString();
        this.networkPassword = in.readString();
        this.networkChannel = in.readInt();
    }

    public static final Parcelable.Creator<BridgeConfig> CREATOR = new Parcelable.Creator<BridgeConfig>() {
        public BridgeConfig createFromParcel(Parcel source) {
            return new BridgeConfig(source);
        }

        public BridgeConfig[] newArray(int size) {
            return new BridgeConfig[size];
        }
    };

    /**
     * Returns if the BridgeConfig is valid -- meaning all fields are set such that a bridge could
     * actually be provisioned
     *
     * @return troo or false
     */
    public boolean isValid() {
        return bridgeSSID != null &&
                networkSSID != null &&
                networkSecurity != null &&
                networkChannel != 0;
    }

    /**
     * Sets the networkSSID, Security, and Channel fields of this config object from the supplied
     * ScanResult
     *
     * @param scanResult ScanResult to retrieve networkSSID/Security/Channel from
     */
    public void setNetworkFieldsFromScanResult(ScanResult scanResult) {
        if (scanResult == null) return;

        // networkSSID
        this.networkSSID = scanResult.SSID;

        // Security - No Security:0, WEP:1, WPA2:4, WPA/WPA2 Mixed:5
        String capabilities = scanResult.capabilities;
        if (capabilities.contains("WPA/WPA2")) {
            setNetworkSecurity("5");
        } else if (capabilities.contains("WPA2")) {
            setNetworkSecurity("4");
        } else if (capabilities.contains("WPA")) {
            setNetworkSecurity("1");
        } else {
            setNetworkSecurity("0");
        }

        // Channel
        setNetworkChannel(NetworkUtils.getChannelFromFrequency(scanResult.frequency));
    }
}
