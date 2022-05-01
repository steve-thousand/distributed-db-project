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

public class SimpleReplicationService implements ReplicationService{

    private static final Logger logger = LoggerFactory.getLogger(SimpleReplicationService.class);

    private final ReplicationHandler replicationHandler;

    private final RegistryClient registryClient;

    private final String name;

    public SimpleReplicationService(ReplicationHandler replicationHandler, RegistryClient registryClient, String name) {
        this.replicationHandler = replicationHandler;
        this.registryClient = registryClient;
        this.name = name;
    }

    @Override
    public void replicate(Replicatable replicatable) throws ReplicationException {
        try {
            logger.info("Beginning replication of {}", replicatable);

            List<RegistryEntry> registryEntries = registryClient.getRegistry().getRegistryEntries();

            //replicate to self
            logger.info("Replicating {} to self", replicatable);
            replicationHandler.handleReplication(replicatable);

            //replicate to others
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

            logger.info("Successfully replicated {}", replicatable);
        }catch(Exception e) {
            throw new ReplicationException("Error encountered during replication", e, replicatable);
        }
    }
}
