package io.steve000.distributed.db.cluster.replication;

import io.steve000.distributed.db.common.http.HttpRequest;
import io.steve000.distributed.db.common.http.HttpResponse;
import io.steve000.distributed.db.registry.api.RegistryEntry;
import io.steve000.distributed.db.registry.client.RegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleReplicationService implements ReplicationService {

    private static final Logger logger = LoggerFactory.getLogger(SimpleReplicationService.class);

    private final ReplicationHandler replicationHandler;

    private final RegistryClient registryClient;

    private final String name;

    private final Lock writeLock;

    public SimpleReplicationService(ReplicationHandler replicationHandler, RegistryClient registryClient, String name) {
        this.replicationHandler = replicationHandler;
        this.registryClient = registryClient;
        this.name = name;
        writeLock = new ReentrantLock(true);
    }

    /**
     * I believe this is the log replication method of the Raft algorithm.
     *
     * @param replicatable The replicatable entry that will be committed to all followers.
     * @throws ReplicationException
     * @see <a href="https://en.wikipedia.org/wiki/Raft_(algorithm)#Log_replication">Raft algorithm</a>
     */
    @Override
    public void replicate(Replicatable replicatable) throws ReplicationException {
        try {
            writeLock.lock();

            logger.info("Beginning replication of {}", replicatable);

            List<RegistryEntry> registryEntries = registryClient.getRegistry().getRegistryEntries();

            //append to local log
            replicationHandler.appendToLog(replicatable);

            //send to followers
            replicateToFollowers(replicatable, registryEntries);

            //commit to local log and apply to state
            replicationHandler.commit();

            //instruct followers to commit
            commitFollowers(registryEntries);

            logger.info("Successfully replicated {}", replicatable);
        } catch (Exception e) {
            throw new ReplicationException("Error encountered during replication", e, replicatable);
        } finally {
            writeLock.unlock();
        }
    }

    private void replicateToFollowers(Replicatable replicatable, List<RegistryEntry> registryEntries) throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        for (RegistryEntry entry : registryEntries) {
            if (name.equals(entry.getName())) {
                continue;
            }
            executorService.execute(() -> replicateToFollower(replicatable, entry));
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
    }

    private void commitFollowers(List<RegistryEntry> registryEntries) throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        for (RegistryEntry entry : registryEntries) {
            if (name.equals(entry.getName())) {
                continue;
            }
            executorService.execute(() -> commitFollower(entry));
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
    }

    private static void replicateToFollower(Replicatable replicatable, RegistryEntry follower) {
        try {
            logger.info("Replicating {} to {}", replicatable, follower);
            final String url = "http://" + follower.getHost() + ":" + follower.getPort() + "/cluster/replication";
            try (HttpResponse response = new HttpRequest.Builder(url).post(replicatable)) {
                if (response.getResponseCode() != HttpURLConnection.HTTP_ACCEPTED) {
                    throw new IOException("Bad response code: " + response.getResponseCode());
                }
            }
        } catch (Exception e) {
            logger.error("Error replicating {} to entry {}", replicatable, follower, e);
        }
    }

    private static void commitFollower(RegistryEntry follower) {
        try {
            logger.info("Sending commit to follower {}", follower);
            final String url = "http://" + follower.getHost() + ":" + follower.getPort() + "/cluster/replication/commit";
            try (HttpResponse response = new HttpRequest.Builder(url).post()) {
                if (response.getResponseCode() != HttpURLConnection.HTTP_ACCEPTED) {
                    throw new IOException("Bad response code: " + response.getResponseCode());
                }
            }
        } catch (Exception e) {
            logger.error("Error committing {}", follower, e);
        }
    }
}
