package io.steve000.distributed.db.cluster.replication;

public class ReplicationException extends Exception{

    private final Replicatable replicatable;

    public ReplicationException(Throwable e, Replicatable replicatable) {
        super(e);
        this.replicatable = replicatable;
    }

    public ReplicationException(String message, Throwable cause, Replicatable replicatable) {
        super(message, cause);
        this.replicatable = replicatable;
    }

    public Replicatable getReplicatable() {
        return replicatable;
    }
}
