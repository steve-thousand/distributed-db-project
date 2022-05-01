package io.steve000.distributed.db.registry.client;

public class RegistryException extends Exception{

    public RegistryException(String message) {
        super(message);
    }

    public RegistryException(Throwable e) {
        super(e);
    }
}
