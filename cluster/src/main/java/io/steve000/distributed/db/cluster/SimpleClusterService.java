package io.steve000.distributed.db.cluster;

import com.sun.net.httpserver.HttpServer;
import io.steve000.distributed.db.cluster.election.ElectionService;
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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

public class SimpleClusterService implements ClusterService {

    private static final Logger logger = LoggerFactory.getLogger(SimpleClusterService.class);

    private static final Duration LATEST_HEARTBEAT_THRESHOLD = Duration.of(60, ChronoUnit.SECONDS);

    private static final int CLUSTER_THREAD_PERIOD_MS = 30000;

    private final ElectionService electionService;

    private final RegistryClient registryClient;

    private ReplicationStatus replicationStatus;

    private Leader leader;

    private LocalDateTime latestHeartBeat = null;

    private boolean registered = false;

    private Executor executor;

    public SimpleClusterService(ElectionService electionService, RegistryClient registryClient, String name) {
        this.electionService = electionService;
        this.registryClient = registryClient;
        this.replicationStatus = new ReplicationStatus(name, 0);
    }

    @Override
    public void bind(HttpServer httpServer) {
        httpServer.createContext("/cluster", new ClusterHttpHandler(this));
    }

    @Override
    public Leader getLeader() {
        try {
            //do we have leader?
            if (leader == null || !isLeaderLive()) {
                //is there a leader out there somewhere?
                RegistryEntry entry = registryClient.getLeader();
                if (entry != null) {
                    //pretty sure if we have to ask, it means we aren't the leader
                    leader = new Leader(entry.getName(), false);
                    return leader;
                }

                //if no live leader, elect one
                logger.debug("Begin leader election process");
                replicationStatus = replicationStatus.increment();
                this.leader = electionService.electLeader(replicationStatus);
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
            if(registered) {
                logger.warn("Already registered, cannot register twice.");
                return;
            }
            registered = true;
            registryClient.register(name, adminPort);
            getLeader(); //if there is a leader in the cluster, identify it. else we are leader
            executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                while(true) {
                    try{
                        Leader leader = getLeader();
                        if (leader.isSelf()) {
                            //heart beat to followers
                            sendHeartBeats();
                        }
                        sleep(CLUSTER_THREAD_PERIOD_MS);
                    } catch (Exception e) {
                        logger.error("Cluster thread error", e);
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unregister() {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public void handleHeartBeat(HeartBeat heartBeat) {
        logger.info("Received leader heartbeat.");
        if(leader != null && leader.isSelf()){
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
                if(entry.getName().equals(heartBeat.getName())){
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
        }catch(IOException | InterruptedException e) {
            logger.error("Failed sending heartbeats", e);
            throw new RuntimeException(e);
        }
    }

    private boolean isLeaderLive() {
        if(leader != null && leader.isSelf()) {
            return true;
        }
        if(latestHeartBeat == null) {
            return false;
        }
        Duration lastHeartBeatAge = Duration.between(this.latestHeartBeat, LocalDateTime.now());
        return lastHeartBeatAge.compareTo(LATEST_HEARTBEAT_THRESHOLD) < 0;
    }
}
