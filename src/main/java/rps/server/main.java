package rps.server;

import rps.server.core.di.DIContainer;
import rps.server.core.di.DependencyInstaller;
import rps.server.module.abstraction.IServerService;

public class Main {
    public static void main(String[] args) {
        DIContainer container = new DIContainer();
        DependencyInstaller.install(container);

        IServerService serverService = container.resolve(IServerService.class);
        serverService.start();
    }
}
