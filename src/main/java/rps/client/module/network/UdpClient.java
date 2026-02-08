package rps.client.module.network;

import rps.client.core.config.ConfigManager;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class UdpClient {

    private final DatagramSocket socket;
    private final InetAddress serverAddress;
    private final int serverPort;

    public UdpClient(ConfigManager config) {
        try {
            this.socket = new DatagramSocket();
            this.serverAddress = InetAddress.getByName(config.serverIp());
            this.serverPort = config.serverPort();
            System.out.println("✅ UDP Client will send to " + config.serverIp() + ":" + config.serverPort());
        } catch (Exception e) {
            throw new RuntimeException("Failed to init UDP client", e);
        }
    }

    public void send(String message) {
        try {
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException("UDP send failed", e);
        }
    }

    public String receive() {
        try {
            byte[] buffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            return new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new RuntimeException("UDP receive failed", e);
        }
    }

    public void close() {
        socket.close();
    }
}
