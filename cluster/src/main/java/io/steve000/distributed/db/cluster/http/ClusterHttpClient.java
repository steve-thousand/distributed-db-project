package io.steve000.distributed.db.cluster.http;

import io.steve000.distributed.db.cluster.HeartBeat;
import io.steve000.distributed.db.common.JSON;
import io.steve000.distributed.db.registry.api.RegistryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClusterHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(ClusterHttpClient.class);

    private final String name;

    private final long clusterThreadPeriodMs;

    public ClusterHttpClient(String name, long clusterThreadPeriodMs) {
        this.name = name;
        this.clusterThreadPeriodMs = clusterThreadPeriodMs;
    }

    public void sendHeartBeats(List<RegistryEntry> registryEntries) {
        try {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            HeartBeat heartBeat = new HeartBeat(name);
            for (RegistryEntry entry : registryEntries) {
                if (entry.getName().equals(name)) {
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
                        if (responseCode != HttpURLConnection.HTTP_CREATED) {
                            throw new IOException("Bad response code: " + responseCode);
                        }
                    } catch (IOException e) {
                        logger.error("Failed sending heartbeat to entry {}", entry, e);
                    }
                });
            }
            executor.shutdown();
            executor.awaitTermination(clusterThreadPeriodMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("Failed sending heartbeats", e);
            throw new RuntimeException(e);
        }
    }

}
