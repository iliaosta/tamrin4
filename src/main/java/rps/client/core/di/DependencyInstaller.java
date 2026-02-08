package rps.client.core.di;

import rps.client.core.config.ConfigManager;
import rps.client.module.abstraction.IClientService;
import rps.client.module.business.ClientService;
import rps.client.module.network.UdpClient;

public class DependencyInstaller {

    public static void install(DIContainer container) {
        // Config
        container.register(ConfigManager.class, ConfigManager.class);

        // Network
        container.register(UdpClient.class, UdpClient.class);

        // Service
        container.register(IClientService.class, ClientService.class);
    }
}
