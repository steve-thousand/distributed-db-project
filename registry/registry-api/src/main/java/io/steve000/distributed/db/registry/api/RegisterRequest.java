package io.steve000.distributed.db.registry.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterRequest {

    private final int port;

    @JsonCreator
    public RegisterRequest(@JsonProperty("port") int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
