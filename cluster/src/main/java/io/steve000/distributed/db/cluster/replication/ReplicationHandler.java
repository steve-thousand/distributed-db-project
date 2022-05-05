package io.steve000.distributed.db.cluster.replication;

import io.steve000.distributed.db.cluster.replication.log.ReplicationLog;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles replication logic that is performed within each node, including the leader. Replication
 * logging for durability happens in this layer.
 */
public class ReplicationHandler implements Closeable {

    private final ReplicationLog replicationLog;

    private final ExecutorService executorService;

    private boolean stopped = false;

    public ReplicationHandler(ReplicationLog replicationLog, ReplicationReceiver replicationReceiver) {
        this.replicationLog = replicationLog;
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                while (!stopped) {
                    Replicatable replicatable = replicationLog.blockingRead();
                    replicationReceiver.receiveReplication(replicatable);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        executorService.shutdown();
    }

    public void appendToLog(Replicatable replicatable) throws ReplicationLogException {
        replicationLog.append(replicatable);
    }

    public void commit() throws ReplicationLogException {
        replicationLog.commit();
    }

    @Override
    public void close() throws IOException {
        replicationLog.close();
        stopped = true;
        executorService.shutdownNow();
    }
}
