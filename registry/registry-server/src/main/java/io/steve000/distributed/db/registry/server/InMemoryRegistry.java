package io.steve000.distributed.db.registry.server;

import io.steve000.distributed.db.registry.api.RegistryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class InMemoryRegistry implements Registry {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryRegistry.class);

    private final List<RegistryEntry> records = new ArrayList<>();

    @Override
    public void register(RegistryEntry record) {
        records.add(record);
        logger.info("Registered node {} at IP {}", record.getName(), record.getHost() + ":" + record.getPort());
    }

    @Override
    public List<RegistryEntry> getRecords() {
        return records;
    }
}
