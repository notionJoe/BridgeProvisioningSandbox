package com.getnotion.android.bridgeprovisioner.network.bridge.provision;

import javax.crypto.spec.SecretKeySpec;

public class SecureSessionData {
    private final BridgeConfig bridgeConfig;
    private final SecretKeySpec secretKeySpec;
    private final byte[] sharedSecret;
    private final String sessionId;

    public SecureSessionData(BridgeConfig bridgeConfig, SecretKeySpec secretKeySpec, byte[] sharedSecret, String sessionId) {
        this.bridgeConfig = bridgeConfig;
        this.secretKeySpec = secretKeySpec;
        this.sharedSecret = sharedSecret;
        this.sessionId = sessionId;
    }

    public BridgeConfig getBridgeConfig() {
        return bridgeConfig;
    }

    public SecretKeySpec getSecretKeySpec() {
        return secretKeySpec;
    }

    public byte[] getSharedSecret() {
        return sharedSecret;
    }

    public String getSessionId() {
        return sessionId;
    }
}
