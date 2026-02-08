package rps.server.module.network;

import rps.server.core.config.ConfigManager;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class UdpServer {
    private final DatagramSocket socket;

    public UdpServer(ConfigManager config) {
        try {
            this.socket = new DatagramSocket(config.serverPort());

            // هر 1 ثانیه اگر پیامی نیامد، receive یک SocketTimeoutException می‌دهد
            this.socket.setSoTimeout(1000);

            System.out.println("✅ UDP Server bound to port: " + config.serverPort());
        } catch (SocketException e) {
            throw new RuntimeException("Failed to start UDP server", e);
        }
    }

    /**
     * اگر پیام برسد ReceivedMessage برمی‌گرداند.
     * اگر در بازه 1 ثانیه پیامی نرسد null برمی‌گرداند (برای چک کردن timeoutها).
     */
    public ReceivedMessage receiveOrNull() {
        try {
            byte[] buffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
            return new ReceivedMessage(msg, packet.getAddress(), packet.getPort());
        } catch (SocketTimeoutException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException("UDP receive failed", e);
        }
    }

    public void send(String message, InetAddress address, int port) {
        try {
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException("UDP send failed", e);
        }
    }

    public void close() {
        socket.close();
    }

    public record ReceivedMessage(String message, InetAddress address, int port) {}
}
