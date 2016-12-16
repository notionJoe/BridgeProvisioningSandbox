package com.getnotion.android.bridgeprovisioner.network.bridge.provision;

import android.annotation.TargetApi;
import android.net.Network;
import android.os.Build;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.SocketFactory;

public class BoundSocketFactory extends SocketFactory {
    private Network network;

    protected BoundSocketFactory(Network network) {
        super();
        this.network = network;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Socket createSocket() throws IOException {
        Socket socket = super.createSocket();
        network.bindSocket(socket);
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return new Socket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return new Socket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return new Socket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return new Socket(address, port, localAddress, localPort);
    }
}
