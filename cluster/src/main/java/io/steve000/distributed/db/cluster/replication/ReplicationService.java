package io.steve000.distributed.db.cluster.replication;

public interface ReplicationService {

    void replicate(Replicatable replicatable) throws ReplicationException;

}
