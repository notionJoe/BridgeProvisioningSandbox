package com.getnotion.android.bridgeprovisioner.network;


import android.getnotion.android.bridgeprovisioner.BuildConfig;

import com.getnotion.android.bridgeprovisioner.utils.StringUtils;

import java.util.concurrent.TimeUnit;

import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;


public class NotionHttpClientBuilder {

    private static final String HOSTNAME = StringUtils.stripHttpsProtocol(BuildConfig.ENVIRONMENT.getEndpoint());

    private static NotionHttpClientBuilder instance;

    private final CertificatePinner certificatePinner;

    public static OkHttpClient.Builder getInstance() {
        if (instance == null) {
            instance = new NotionHttpClientBuilder();
        }
        return instance.getHttpClientBuilder();
    }

    private OkHttpClient.Builder getHttpClientBuilder() {
        // TODO: Temporarily set timeouts to zero to match previous behavior
        return new OkHttpClient.Builder()
                .connectTimeout(0, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .certificatePinner(certificatePinner);
    }

    private NotionHttpClientBuilder() {
        // TODO: UPDATE ME!
        this.certificatePinner = new CertificatePinner.Builder()
                .add(HOSTNAME, "sha256/1SK9/naINHSXs5NJb1CUEkhw3Hi76vZRG6OYDlTXvy4=")
                .add(HOSTNAME, "sha256/klO23nT2ehFDXCfx3eHTDRESMz3asj1muO+4aIdjiuY=")
                .add(HOSTNAME, "sha256/grX4Ta9HpZx6tSHkmCrvpApTQGo67CYDnvprLg5yRME=")
                .build();
    }
}
