package io.steve000.distributed.db.cluster.replication;

/**
 * The behavior to be performed when a replication event is received by a node is defined here. So
 * this is where we would, for example, handle writing to a database.
 */
public interface ReplicationHandler {

    void handleReplication(Replicatable replicatable);

}
