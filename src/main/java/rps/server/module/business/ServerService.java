package rps.server.module.business;

import rps.server.module.abstraction.IServerService;

public class ServerService implements IServerService {

    @Override
    public void start() {
        System.out.println("ServerService started (placeholder).");
        // مرحله بعدی: UDP server واقعی را اینجا راه می‌اندازیم
    }
}
