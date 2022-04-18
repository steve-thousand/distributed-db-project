package io.steve000.distributed.db.registry.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegistryEntry {

    private final String name;

    private final String host;

    private final int port;

    @JsonCreator
    public RegistryEntry(@JsonProperty("name") String name,
                         @JsonProperty("host") String host,
                         @JsonProperty("port") int port) {
        this.name = name;
        this.host = host;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "RegistryEntry{" +
                "name='" + name + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
