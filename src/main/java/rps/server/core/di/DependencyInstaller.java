package rps.server.core.di;

import rps.server.module.abstraction.IServerService;
import rps.server.module.business.ServerService;

public class DependencyInstaller {

    public static void install(DIContainer container) {
        // نمونه آماده/الگو
        container.register(IServerService.class, ServerService.class);

        // بعداً سرویس‌های دیگر را اینجا اضافه می‌کنیم:
        // container.register(IGameService.class, GameService.class);
        // container.register(ISessionManager.class, SessionManager.class);
    }
}
