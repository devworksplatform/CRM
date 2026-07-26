package com.petsfort.jrpc;

import com.jay.config.JServer;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Lifecycle entry point loaded by JRPC Studio. */
public final class PetsFortApplication {
    private final AtomicBoolean initialized = new AtomicBoolean();
    private CrmService service;

    public PetsFortApplication() {
    }

    public void init(JServer server) {
        Objects.requireNonNull(server, "server");
        if (!initialized.compareAndSet(false, true)) {
            throw new IllegalStateException("PetsFort CRM application is already initialized");
        }
        try {
            service = CrmService.open(CrmConfiguration.fromEnvironment());
            for (CrmRpc rpc : CrmRpc.values()) {
                server.registerRpc(rpc, service.handlerFor(rpc));
            }
        } catch (RuntimeException error) {
            initialized.set(false);
            if (service != null) {
                service.close();
                service = null;
            }
            throw error;
        }
    }

    public void close() {
        if (initialized.compareAndSet(true, false)) {
            CrmService active = service;
            service = null;
            if (active != null) {
                active.close();
            }
        }
    }
}
