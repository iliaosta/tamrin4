package rps.client;

import rps.client.core.di.DIContainer;
import rps.client.core.di.DependencyInstaller;
import rps.client.module.abstraction.IClientService;

public class Main {
    public static void main(String[] args) {
        DIContainer container = new DIContainer();
        DependencyInstaller.install(container);

        IClientService clientService = container.resolve(IClientService.class);
        clientService.start();
    }
}
