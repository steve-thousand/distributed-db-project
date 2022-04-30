package io.steve000.distributed.db.cluster;

import io.steve000.distributed.db.cluster.http.ClusterHttpClient;
import io.steve000.distributed.db.registry.api.RegistryEntry;
import io.steve000.distributed.db.registry.api.RegistryResponse;
import io.steve000.distributed.db.registry.client.RegistryClient;
import io.steve000.distributed.db.registry.client.RegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NodeThread implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(NodeThread.class);

    private final ClusterService clusterService;

    private final RegistryClient registryClient;

    private final ClusterHttpClient clusterHttpClient;

    private final long clusterThreadPeriodMs;

    private ScheduledExecutorService executorService;

    public NodeThread(ClusterService clusterService, RegistryClient registryClient, ClusterHttpClient clusterHttpClient, long clusterThreadPeriodMs) {
        this.clusterService = clusterService;
        this.registryClient = registryClient;
        this.clusterHttpClient = clusterHttpClient;
        this.clusterThreadPeriodMs = clusterThreadPeriodMs;
    }

    public void run() {
        executorService = Executors.newScheduledThreadPool(2);

        //one thread for periodic registry heartbeats
        executorService.scheduleAtFixedRate(() -> {
            try {
                registryClient.sendRegistryHeartbeat();
            } catch (RegistryException e) {
                logger.error("Cluster thread error", e);
            }
        }, 0, clusterThreadPeriodMs, TimeUnit.MILLISECONDS);

        //one thread for leader duties, which could interfere with registry heartbeats if it were in the same thread
        executorService.scheduleAtFixedRate(() -> {
            try {
                Leader leader = clusterService.getLeader();
                if (leader.isSelf()) {
                    //heart beat to followers
                    logger.debug("Node {} sending leader heartbeats...", leader.getName());
                    sendHeartBeats();
                }
            } catch (Exception e) {
                logger.error("Cluster thread error", e);
                throw new RuntimeException(e);
            }
        }, 0, clusterThreadPeriodMs, TimeUnit.MILLISECONDS);
    }

    public void sendHeartBeats() {
        try {
            RegistryResponse response = registryClient.getRegistry();
            List<RegistryEntry> registryEntries = response.getRegistryEntries();
            clusterHttpClient.sendHeartBeats(registryEntries);
        } catch (IOException e) {
            logger.error("Failed sending leader heartbeats", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        logger.info("Shutting down NodeThread.");
        executorService.shutdownNow();
    }
}
