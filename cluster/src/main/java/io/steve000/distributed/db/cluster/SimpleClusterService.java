package io.steve000.distributed.db.cluster;

import com.sun.net.httpserver.HttpServer;
import io.steve000.distributed.db.cluster.election.Elector;
import io.steve000.distributed.db.cluster.http.ClusterHttpClient;
import io.steve000.distributed.db.cluster.http.ClusterHttpHandler;
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

    private final Duration LATEST_HEARTBEAT_THRESHOLD;

    private final ClusterConfig config;

    private final Elector elector;

    private final RegistryClient registryClient;

    private final ClusterHttpClient clusterHttpClient;

    private ReplicationStatus replicationStatus;

    private Leader leader;

    private LocalDateTime latestHeartBeat = null;

    private NodeThread nodeThread;

    public SimpleClusterService(ClusterConfig config, Elector elector, RegistryClient registryClient, ClusterHttpClient clusterHttpClient, String name) {
        this.config = config;
        this.elector = elector;
        this.registryClient = registryClient;
        this.clusterHttpClient = clusterHttpClient;
        this.replicationStatus = new ReplicationStatus(name, 0);
        LATEST_HEARTBEAT_THRESHOLD = Duration.of(config.getCusterThreadPeriodMs() * 5, ChronoUnit.MILLIS);
    }

    @Override
    public void bind(HttpServer httpServer) {
        httpServer.createContext("/cluster", new ClusterHttpHandler(this));
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
                replicationStatus = replicationStatus.increment();
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

    private boolean isLeaderLive() {
        if (leader != null && leader.isSelf()) {
            return true;
        }
        if (latestHeartBeat == null) {
            return false;
        }
        Duration lastHeartBeatAge = Duration.between(this.latestHeartBeat, LocalDateTime.now());
        return lastHeartBeatAge.compareTo(LATEST_HEARTBEAT_THRESHOLD) < 0;
    }

    @Override
    public void close() {
        if (nodeThread != null) {
            logger.info("Shutting down SimpleClusterService.");
            nodeThread.close();
        }
    }
}
