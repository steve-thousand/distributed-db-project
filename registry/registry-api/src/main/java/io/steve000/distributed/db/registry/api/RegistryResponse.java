package io.steve000.distributed.db.registry.api;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;

public class RegistryResponse {

    private final List<RegistryEntry> registryEntries;

    @JsonCreator
    public RegistryResponse(List<RegistryEntry> registryEntries) {
        this.registryEntries = registryEntries;
    }

    public List<RegistryEntry> getRegistryEntries() {
        return registryEntries;
    }
}
