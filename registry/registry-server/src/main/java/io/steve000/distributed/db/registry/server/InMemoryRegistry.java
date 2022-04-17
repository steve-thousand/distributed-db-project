package io.steve000.distributed.db.registry.server;

import io.steve000.distributed.db.registry.api.RegistryEntry;

import java.util.ArrayList;
import java.util.List;

public class InMemoryRegistry implements Registry {

    private final List<RegistryEntry> records = new ArrayList<>();

    @Override
    public void register(RegistryEntry record) {
        records.add(record);
    }

    @Override
    public List<RegistryEntry> getRecords() {
        return records;
    }
}
