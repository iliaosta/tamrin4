package rps.server.module.model;

import java.net.InetAddress;

public class PlayerInfo {
    private final InetAddress address;
    private final int port;

    public PlayerInfo(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String key() {
        return address.getHostAddress() + ":" + port;
    }
}
