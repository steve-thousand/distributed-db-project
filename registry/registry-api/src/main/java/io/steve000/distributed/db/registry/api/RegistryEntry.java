package io.steve000.distributed.db.registry.api;

import com.fasterxml.jackson.annotation.JsonCreator;

public class RegistryEntry {

    private final String host;

    private final int port;

    @JsonCreator
    public RegistryEntry(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

}
