package io.steve000.distributed.db.cluster;

import com.sun.net.httpserver.HttpServer;
import io.steve000.distributed.db.cluster.election.Elector;
import io.steve000.distributed.db.cluster.http.ClusterHttpClient;
import io.steve000.distributed.db.cluster.http.ClusterHttpHandler;
import io.steve000.distributed.db.cluster.replication.ReplicationHandler;
import io.steve000.distributed.db.cluster.replication.ReplicationHttpHandler;
import io.steve000.distributed.db.cluster.replication.ReplicationService;
import io.steve000.distributed.db.registry.client.RegistryClient;
import io.steve000.distributed.db.registry.client.RegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimpleClusterService implements ClusterService {

    private static final Logger logger = LoggerFactory.getLogger(SimpleClusterService.class);

    private final Duration latestHeartBeatThreshold;

    private final ClusterConfig config;

    private final Elector elector;

    private final RegistryClient registryClient;

    private final ClusterHttpClient clusterHttpClient;

    private final ReplicationStatus replicationStatus;

    private final ReplicationHandler replicationHandler;

    private Leader leader;

    private LocalDateTime latestHeartBeat = null;

    private NodeThread nodeThread;

    private SimpleClusterService(Builder builder) {
        this.config = builder.config;
        this.elector = builder.elector;
        this.registryClient = builder.registryClient;
        this.clusterHttpClient = builder.clusterHttpClient;
        this.replicationHandler = builder.replicationHandler;
        this.replicationStatus = new ReplicationStatus(builder.config.getName());
        latestHeartBeatThreshold = Duration.of(config.getCusterThreadPeriodMs() * 5, ChronoUnit.MILLIS);
    }

    @Override
    public void bind(HttpServer httpServer) {
        httpServer.createContext("/cluster", new ClusterHttpHandler(this));
        httpServer.createContext("/cluster/replication", new ReplicationHttpHandler(replicationHandler));
    }

    @Override
    public Leader getLeader() {
        try {
            //if we do not yet have a leader, find one (if there is one)
            if (leader == null) {
                //wait for a leader heartbeat
                logger.info("No configured leader, waiting for leader heartbeat");
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(() -> {
                    while (true) {
                        try {
                            if (leader != null) {
                                return;
                            }
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            logger.error("Exception waiting for latest heartbeat", e);
                            throw new RuntimeException(e);
                        }
                    }
                });
                executorService.shutdown();
                executorService.awaitTermination(config.getCusterThreadPeriodMs() * 5, TimeUnit.MILLISECONDS);
                if (leader != null) {
                    logger.info("Assigned leader by heartbeat: {}", leader);
                }
            }

            //if leader is not live, elect new leader
            if (!isLeaderLive()) {
                logger.info("Leader is not live, begin leader election process");
                replicationStatus.increment();
                this.leader = elector.electLeader(replicationStatus);
                logger.info("Leader chosen: {}", leader);
            }

            return leader;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get leader", e);
        }
    }

    @Override
    public synchronized void run() {
        try {
            registryClient.sendRegistryHeartbeat();
            getLeader(); //prime the leader info before running
            nodeThread = new NodeThread(this, registryClient, clusterHttpClient, config.getCusterThreadPeriodMs());
            nodeThread.run();
        } catch (RegistryException e) {
            logger.error("Error un startup", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleHeartBeat(HeartBeat heartBeat) {
        logger.debug("Node {} received leader heartbeat from leader {}.", replicationStatus.getName(), heartBeat.getName());
        if (leader == null || !leader.getName().equals(heartBeat.getName())) {
            leader = new Leader(heartBeat.getName(), false);
            logger.info("New leader chosen: {}", leader);
        }
        leader = new Leader(heartBeat.getName(), false);
        latestHeartBeat = LocalDateTime.now();
    }

    @Override
    public ReplicationService replicationService() {
        throw new RuntimeException("Not implemented!");
    }

    private boolean isLeaderLive() {
        if (leader != null && leader.isSelf()) {
            return true;
        }
        if (latestHeartBeat == null) {
            return false;
        }
        Duration lastHeartBeatAge = Duration.between(this.latestHeartBeat, LocalDateTime.now());
        return lastHeartBeatAge.compareTo(latestHeartBeatThreshold) < 0;
    }

    @Override
    public void close() {
        if (nodeThread != null) {
            logger.info("Shutting down SimpleClusterService.");
            nodeThread.close();
        }
    }

    public static class Builder {

        private ClusterConfig config;

        private Elector elector;

        private RegistryClient registryClient;

        private ClusterHttpClient clusterHttpClient;

        private ReplicationHandler replicationHandler;

        public Builder withConfig(ClusterConfig val) {
            this.config = val;
            return this;
        }

        public Builder withElector(Elector val) {
            this.elector = val;
            return this;
        }

        public Builder withRegistryClient(RegistryClient val) {
            this.registryClient = val;
            return this;
        }

        public Builder withClusterHttpClient(ClusterHttpClient val) {
            this.clusterHttpClient = val;
            return this;
        }

        public Builder withReplicationHandler(ReplicationHandler val) {
            this.replicationHandler = val;
            return this;
        }

        public ClusterService build() {
            if(clusterHttpClient == null) {
                clusterHttpClient = new ClusterHttpClient(config.getName(), config.getCusterThreadPeriodMs());
            }
            return new SimpleClusterService(this);
        }

    }
}
