package io.steve000.distributed.db.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.sleep;

public class NodeThread implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(NodeThread.class);

    private final ClusterService clusterService;

    private final long clusterThreadPeriodMs;

    private boolean stopped = false;

    public NodeThread(ClusterService clusterService, long clusterThreadPeriodMs) {
        this.clusterService = clusterService;
        this.clusterThreadPeriodMs = clusterThreadPeriodMs;
    }

    public void run() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            while (!stopped) {
                try {
                    Leader leader = clusterService.getLeader();
                    if (leader.isSelf()) {
                        //heart beat to followers
                        clusterService.sendHeartBeats();
                    }
                    sleep(clusterThreadPeriodMs);
                } catch (Exception e) {
                    logger.error("Cluster thread error", e);
                    throw new RuntimeException(e);
                }
            }
        });
        executorService.shutdown();
    }

    @Override
    public void close() {
        stopped = true;
    }
}
