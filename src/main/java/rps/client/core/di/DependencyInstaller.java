package rps.client.core.di;

import rps.client.module.abstraction.IClientService;
import rps.client.module.business.ClientService;

public class DependencyInstaller {

    public static void install(DIContainer container) {
        container.register(IClientService.class, ClientService.class);
    }
}
