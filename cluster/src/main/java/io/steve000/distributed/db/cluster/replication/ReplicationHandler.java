package io.steve000.distributed.db.cluster.replication;

import io.steve000.distributed.db.cluster.Leader;
import io.steve000.distributed.db.cluster.replication.log.ReplicationLog;
import io.steve000.distributed.db.common.http.HttpRequest;
import io.steve000.distributed.db.common.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles replication logic that is performed within each node, including the leader. Replication
 * logging for durability happens in this layer.
 */
public class ReplicationHandler implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(ReplicationHandler.class);

    private final ReplicationLog replicationLog;

    private final ExecutorService executorService;

    private boolean stopped = false;

    public ReplicationHandler(ReplicationLog replicationLog, ReplicationReceiver replicationReceiver) {
        this.replicationLog = replicationLog;
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            while (!stopped) {
                try {
                    Replicatable replicatable = replicationLog.blockingRead();
                    replicationReceiver.receiveReplication(replicatable);
                } catch (InterruptedException e) {
                    //TODO
                }
            }
        });
        executorService.shutdown();
    }

    /**
     * Start the process of syncing with the leader node
     */
    public void sync(Leader leader) throws ReplicationLogException {
        try {
            logger.info("Syncing replication log to leader {}", leader);
            final String url = "http://" + leader.getHost() + ":" + leader.getPort() + "/cluster/replication/sync";
            HttpResponse response = new HttpRequest.Builder(url).get();
            replicationLog.sync(response.read());
        } catch (Exception e) {
            throw new ReplicationLogException("Failed to sync replication log from leader " + leader, e);
        }
    }

    public void sync(OutputStream outputStream) throws IOException {
        logger.info("Handling replication log sync request...");
        replicationLog.sync(outputStream);
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
