package com.getnotion.android.bridgeprovisioner.network.bridge.provision;

import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;

import java.util.UUID;

public class Curve25519Provider {

    private final Curve25519 cipher;

    public Curve25519Provider() {
        this.cipher = Curve25519.getInstance(Curve25519.BEST);
    }

    public Curve25519KeyPair getKeyPair() {
        return cipher.generateKeyPair();
    }

    public byte[] getRandomSignature(Curve25519KeyPair keyPair) {
        return cipher.calculateSignature(keyPair.getPrivateKey(), getRandomUuidBytes());
    }

    public byte[] getCurveSharedSecret(byte[] publicKeyBytes, byte[] privateKeyBytes) {
        return cipher.calculateAgreement(publicKeyBytes, privateKeyBytes);
    }

    private byte[] getRandomUuidBytes() {
        return UUID.randomUUID().toString().getBytes();
    }
}
