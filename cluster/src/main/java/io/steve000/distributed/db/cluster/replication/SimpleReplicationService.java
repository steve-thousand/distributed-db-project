package io.steve000.distributed.db.cluster.replication;

import io.steve000.distributed.db.common.JSON;
import io.steve000.distributed.db.registry.api.RegistryEntry;
import io.steve000.distributed.db.registry.client.RegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
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
        writeLock = new ReentrantLock();
    }

    /**
     * I believe this is the log replication method of the Raft algorithm
     *
     * @param replicatable The replicatable entry that will be committed to all followers.
     * @throws ReplicationException
     * @see <a href="https://en.wikipedia.org/wiki/Raft_(algorithm)#Log_replication">Raft algorithm</a>
     */
    @Override
    public void replicate(Replicatable replicatable) throws ReplicationException {
        try {
            //TODO order of operations issues
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

    private void replicateToFollowers(Replicatable replicatable, List<RegistryEntry> registryEntries) {
        for (RegistryEntry entry : registryEntries) {
            if (name.equals(entry.getName())) {
                continue;
            }

            //TODO parallel
            try {
                logger.info("Replicating {} to {}", replicatable, entry);
                URL url = new URL("http://" + entry.getHost() + ":" + entry.getPort() + "/cluster/replication");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                JSON.OBJECT_MAPPER.writeValue(con.getOutputStream(), replicatable);
                int responseCode = con.getResponseCode();
                con.getInputStream().close();
                if (responseCode != HttpURLConnection.HTTP_ACCEPTED) {
                    throw new IOException("Bad response code: " + responseCode);
                }
            } catch (Exception e) {
                logger.error("Error replicating {} to entry {}", replicatable, entry, e);
            }
        }
    }

    private void commitFollowers(List<RegistryEntry> registryEntries) {
        for (RegistryEntry entry : registryEntries) {
            if (name.equals(entry.getName())) {
                continue;
            }

            //TODO parallel
            try {
                logger.info("Sending commit to {}", entry);
                URL url = new URL("http://" + entry.getHost() + ":" + entry.getPort() + "/cluster/replication/commit");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                int responseCode = con.getResponseCode();
                con.getInputStream().close();
                if (responseCode != HttpURLConnection.HTTP_ACCEPTED) {
                    throw new IOException("Bad response code: " + responseCode);
                }
            } catch (Exception e) {
                logger.error("Error committing {}", entry, e);
            }
        }
    }
}
