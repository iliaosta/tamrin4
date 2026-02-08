package rps.server.core.di;

import rps.server.core.config.ConfigManager;
import rps.server.core.logging.Logger;
import rps.server.module.abstraction.IServerService;
import rps.server.module.abstraction.ISessionManager;
import rps.server.module.business.ServerService;
import rps.server.module.business.SessionManager;
import rps.server.module.network.UdpServer;

public class DependencyInstaller {

    public static void install(DIContainer container) {
        // Config
        container.register(ConfigManager.class, ConfigManager.class);

        // Logging
        container.register(Logger.class, Logger.class);

        // Network
        container.register(UdpServer.class, UdpServer.class);

        // Services
        container.register(ISessionManager.class, SessionManager.class);
        container.register(IServerService.class, ServerService.class);
    }
}
