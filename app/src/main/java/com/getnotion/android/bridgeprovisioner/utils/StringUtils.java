package com.getnotion.android.bridgeprovisioner.utils;

public class StringUtils {

    /**
     * @param s to capitalize
     * @return a string with the first character capitalized
     */
    public static String capitalized(String s) {
        if (s == null || s.length() <= 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s);
        sb.setCharAt(0, Character.toUpperCase(s.charAt(0)));
        return sb.toString();
    }

    /**
     * @param s to remove white space from
     * @return a string with no spaces
     */
    public static String removeWhiteSpace(String s) {
        if (s == null) {
            return s;
        }
        return s.replace(" ", "");
    }

    public static String stripHttpsProtocol(String endpoint) {
        if (endpoint.startsWith("https://")) {
            return endpoint.replaceFirst("https://", "");
        }
        return endpoint;
    }
}