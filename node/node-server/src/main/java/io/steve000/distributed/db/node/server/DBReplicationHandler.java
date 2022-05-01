package io.steve000.distributed.db.node.server;

import io.steve000.distributed.db.cluster.replication.Replicatable;
import io.steve000.distributed.db.cluster.replication.ReplicationHandler;
import io.steve000.distributed.db.node.server.db.DBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBReplicationHandler implements ReplicationHandler {

    private final static Logger logger = LoggerFactory.getLogger(DBReplicationHandler.class);

    private final DBService dbService;

    public DBReplicationHandler(DBService dbService) {
        this.dbService = dbService;
    }

    @Override
    public void handleReplication(Replicatable replicatable) {
        final String key = replicatable.getKey();
        final String value = replicatable.getValue();
        logger.info("Setting key {} and value {}", key, value);
        dbService.set(key, value);
    }
}
