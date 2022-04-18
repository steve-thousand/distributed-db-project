package io.steve000.distributed.db.registry.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterRequest {

    private final String name;

    private final int port;

    @JsonCreator
    public RegisterRequest(@JsonProperty("name") String name, @JsonProperty("port") int port) {
        this.name = name;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }
}
