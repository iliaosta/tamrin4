package rps.server.module.business;

import rps.server.module.abstraction.IServerService;
import rps.server.module.abstraction.ISessionManager;
import rps.server.module.model.PlayerInfo;
import rps.server.module.network.UdpServer;

import java.util.List;

public class ServerService implements IServerService {

    private final UdpServer udpServer;
    private final ISessionManager sessionManager;

    public ServerService(UdpServer udpServer, ISessionManager sessionManager) {
        this.udpServer = udpServer;
        this.sessionManager = sessionManager;
    }

    @Override
    public void start() {
        System.out.println("✅ UDP Server started. (Multi-session + Timeout enabled)");

        while (true) {
            // اگر پیام نیاید، null می‌شود و ما timeoutها را چک می‌کنیم
            UdpServer.ReceivedMessage rm = udpServer.receiveOrNull();

            if (rm == null) {
                // tick: چک کردن timeout sessionها
                List<ISessionManager.OutboundMessage> timeoutResponses = sessionManager.checkTimeouts();
                for (ISessionManager.OutboundMessage res : timeoutResponses) {
                    udpServer.send(res.message(), res.to().getAddress(), res.to().getPort());
                }
                continue;
            }

            PlayerInfo player = new PlayerInfo(rm.address(), rm.port());

            List<ISessionManager.OutboundMessage> responses =
                    sessionManager.onMessage(player, rm.message());

            for (ISessionManager.OutboundMessage res : responses) {
                udpServer.send(res.message(), res.to().getAddress(), res.to().getPort());
            }
        }
    }
}
