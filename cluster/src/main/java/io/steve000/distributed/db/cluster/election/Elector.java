package io.steve000.distributed.db.cluster.election;

import io.steve000.distributed.db.cluster.Leader;
import io.steve000.distributed.db.cluster.ReplicationStatus;

public interface Elector {

    /**
     * Begin and await completion of electing a new {@link Leader}
     * @return the new {@link Leader} of the cluster
     */
    Leader electLeader(ReplicationStatus replicationStatus) throws ElectionException;

}
