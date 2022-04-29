package io.steve000.distributed.db.cluster;

import com.sun.net.httpserver.HttpServer;
import io.steve000.distributed.db.cluster.election.Elector;
import io.steve000.distributed.db.common.JSON;
import io.steve000.distributed.db.registry.api.RegistryEntry;
import io.steve000.distributed.db.registry.api.RegistryResponse;
import io.steve000.distributed.db.registry.client.RegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimpleClusterService implements ClusterService {

    private static final Logger logger = LoggerFactory.getLogger(SimpleClusterService.class);

    private final Duration LATEST_HEARTBEAT_THRESHOLD;

    private final ClusterConfig config;

    private final Elector elector;

    private final RegistryClient registryClient;

    private ReplicationStatus replicationStatus;

    private Leader leader;

    private LocalDateTime latestHeartBeat = null;

    private boolean registered = false;

    private NodeThread nodeThread;

    public SimpleClusterService(ClusterConfig config, Elector elector, RegistryClient registryClient, String name) {
        this.config = config;
        this.elector = elector;
        this.registryClient = registryClient;
        this.replicationStatus = new ReplicationStatus(name, 0);
        LATEST_HEARTBEAT_THRESHOLD = Duration.of(config.getCusterThreadPeriodMs() * 3, ChronoUnit.MILLIS);
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
                executorService.awaitTermination(config.getCusterThreadPeriodMs() * 2, TimeUnit.MILLISECONDS);
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
    public synchronized void register(String name, int adminPort) {
        try {
            if (registered) {
                logger.warn("Already registered, cannot register twice.");
                return;
            }
            registered = true;
            registryClient.register(name, adminPort);
            getLeader(); //if there is a leader in the cluster, identify it. else we are leader
            nodeThread = new NodeThread(this, config.getCusterThreadPeriodMs());
            nodeThread.run();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleHeartBeat(HeartBeat heartBeat) {
        logger.debug("Received leader heartbeat.");
        if (leader != null && !leader.isSelf()) {
            logger.info("No longer leader, new leader chosen: {}", leader);
        }
        leader = new Leader(heartBeat.getName(), false);
        latestHeartBeat = LocalDateTime.now();
    }

    @Override
    public void sendHeartBeats() {
        try {
            logger.debug("Sending heartbeats...");
            RegistryResponse response = registryClient.getRegistry();
            List<RegistryEntry> registryEntries = response.getRegistryEntries();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            HeartBeat heartBeat = new HeartBeat(replicationStatus.getName());
            for (RegistryEntry entry : registryEntries) {
                if (entry.getName().equals(heartBeat.getName())) {
                    continue;
                }
                executor.execute(() -> {
                    try {
                        URL url = new URL("http://" + entry.getHost() + ":" + entry.getPort() + "/cluster/heartbeat");
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setRequestMethod("POST");
                        con.setDoOutput(true);
                        JSON.OBJECT_MAPPER.writeValue(con.getOutputStream(), heartBeat);
                        int responseCode = con.getResponseCode();
                        con.getInputStream().close();
                        if (responseCode != 201) {
                            throw new IOException("Bad response code: " + responseCode);
                        }
                    } catch (IOException e) {
                        logger.error("Failed sending heartbeat", e);
                    }
                });
            }
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException e) {
            logger.error("Failed sending heartbeats", e);
            throw new RuntimeException(e);
        }
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
        if(nodeThread != null) {
            nodeThread.close();
        }
    }
}
