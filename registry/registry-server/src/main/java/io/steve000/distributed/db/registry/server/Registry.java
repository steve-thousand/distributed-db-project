package io.steve000.distributed.db.registry.server;

import io.steve000.distributed.db.registry.api.RegistryEntry;

import java.io.Closeable;
import java.util.List;

public interface Registry extends Closeable {

    void register(RegistryEntry record);

    List<RegistryEntry> getRecords();

}
