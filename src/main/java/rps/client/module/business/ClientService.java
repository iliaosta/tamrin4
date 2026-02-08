package rps.client.module.business;

import rps.client.module.abstraction.IClientService;

public class ClientService implements IClientService {

    @Override
    public void start() {
        System.out.println("ClientService started (placeholder).");
        // مرحله بعد: UDP client واقعی اینجا
    }
}
