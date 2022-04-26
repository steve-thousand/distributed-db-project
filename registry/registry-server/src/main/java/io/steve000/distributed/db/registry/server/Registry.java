package io.steve000.distributed.db.registry.server;

import io.steve000.distributed.db.registry.api.RegistryEntry;

import java.util.List;

public interface Registry {

    void register(RegistryEntry record);

    List<RegistryEntry> getRecords();

    void registerLeader(String name);

    RegistryEntry getLeader();

}
