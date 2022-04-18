package io.steve000.distributed.db.registry.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RegistryResponse {

    private final List<RegistryEntry> registryEntries;

    @JsonCreator
    public RegistryResponse(@JsonProperty("registryEntries") List<RegistryEntry> registryEntries) {
        this.registryEntries = registryEntries;
    }

    public List<RegistryEntry> getRegistryEntries() {
        return registryEntries;
    }

    @Override
    public String toString() {
        return "RegistryResponse{" +
                "registryEntries=" + registryEntries +
                '}';
    }
}
