package io.steve000.distributed.db.registry.server;

import io.steve000.distributed.db.registry.api.RegistryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InMemoryRegistry implements Registry {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryRegistry.class);

    private static final Duration EXPIRE_AGE = Duration.of(5, ChronoUnit.SECONDS);

    private final Map<String, RegistryEntry> records = new HashMap<>();

    private final Map<String, LocalDateTime> lastRegistryTime = new HashMap<>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    private ScheduledExecutorService executorService;

    public InMemoryRegistry() {
        runCleanupThread();
    }

    private void runCleanupThread() {
        final Lock writeLock = readWriteLock.writeLock();
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            writeLock.lock();
            try {
                LocalDateTime now = LocalDateTime.now();
                List<String> expired = new ArrayList<>();
                for (Map.Entry<String, LocalDateTime> entry : lastRegistryTime.entrySet()) {
                    Duration age = Duration.between(entry.getValue(), now);
                    if (age.compareTo(EXPIRE_AGE) > 0) {
                        expired.add(entry.getKey());
                    }
                }
                for (String name : expired) {
                    logger.info("Node {} is expired", name);
                    records.remove(name);
                    lastRegistryTime.remove(name);
                }
            } catch (Exception e) {
                logger.error("Error in registry cleanup thread");
            } finally {
                writeLock.unlock();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void register(RegistryEntry record) {
        Lock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            String name = record.getName();
            if (!records.containsKey(name)) {
                logger.info("Registered node {} at IP {}", record.getName(), record.getHost() + ":" + record.getPort());
            }
            records.put(record.getName(), record);
            lastRegistryTime.put(record.getName(), LocalDateTime.now());
        }finally {
            writeLock.unlock();
        }
    }

    @Override
    public List<RegistryEntry> getRecords() {
        Lock readLock = readWriteLock.readLock();
        readLock.lock();
        try {
            return new ArrayList<>(records.values());
        }finally {
            readLock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        if(executorService != null){
            executorService.shutdownNow();
        }
    }
}
