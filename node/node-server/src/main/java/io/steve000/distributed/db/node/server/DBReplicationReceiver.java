package io.steve000.distributed.db.node.server;

import io.steve000.distributed.db.cluster.replication.Replicatable;
import io.steve000.distributed.db.cluster.replication.ReplicationReceiver;
import io.steve000.distributed.db.node.server.db.DBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBReplicationReceiver implements ReplicationReceiver {

    private final static Logger logger = LoggerFactory.getLogger(DBReplicationReceiver.class);

    private final DBService dbService;

    public DBReplicationReceiver(DBService dbService) {
        this.dbService = dbService;
    }

    @Override
    public void receiveReplication(Replicatable replicatable) {
        final String key = replicatable.getKey();
        final String value = replicatable.getValue();
        logger.info("Setting key {} and value {}", key, value);
        dbService.set(key, value);
    }
}
