package com.getnotion.android.bridgeprovisioner.network.bridge.provision;

import org.json.JSONException;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class CryptoUtils {

    public static final String CIPHER_TRANSFORMATION = "AES/CTR/NoPadding";
    public static final String SECRET_KEY_ALGORITHM = "AES";
    public static final String ENCODING = "UTF-8";
    public static final String NULL_TERMINATOR = "\u0000";

    private static final int AES_BLOCK_BYTES = 16;

    public static boolean isChecksumValid(String checksum, byte[] randomSig) throws JSONException, NoSuchAlgorithmException, DigestException {
        byte[] digested = CryptoUtils.getSha512Checksum(randomSig);
        String clientRandomSigHexCheckSum = convertBytesToHex(digested);

        return checksum.equals(clientRandomSigHexCheckSum);
    }

    public static String convertBytesToHex(byte[] arrayBytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte arrayByte : arrayBytes) {
            stringBuilder.append(Integer.toString((arrayByte & 0xff) + 0x100, 16).substring(1));
        }
        return stringBuilder.toString();
    }

    public static byte[] getSha512Checksum(byte[] dataToHash) throws NoSuchAlgorithmException, DigestException {
        return MessageDigest.getInstance("SHA-512").digest(dataToHash);
    }

    public static byte[] generateRandomIv() {
        byte[] randomIv = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(randomIv);
        return randomIv;
    }

    public static byte[] padPayload(byte[] payload) {
        int payloadSize = payload.length;
        int payloadRemainder = payloadSize % AES_BLOCK_BYTES;

        if (payloadSize > 0 && payloadRemainder == 0) {
            return payload;
        }

        int padding = AES_BLOCK_BYTES - payloadRemainder;
        return Arrays.copyOf(payload, payloadSize + padding);
    }

}
