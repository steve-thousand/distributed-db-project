package io.steve000.distributed.db.node.server.cluster.election;

import io.steve000.distributed.db.node.server.cluster.Leader;
import io.steve000.distributed.db.node.server.cluster.ReplicationStatus;
import io.steve000.distributed.db.node.server.cluster.election.api.ElectionRequest;
import io.steve000.distributed.db.node.server.cluster.election.api.VoteResponse;

/**
 * Yaaaaay voting.
 * <p>
 * Uh so my thinking here is that every node periodically checks if there is a leader (maybe checks on leader health?).
 * If no healthy leader, start a vote. Then we need to setup an algorithm for deciding outcome of election.
 */
public interface ElectionService {

    /**
     * Begin and await completion of electing a new {@link Leader}
     * @return the new {@link Leader} of the cluster
     */
    Leader electLeader(ReplicationStatus replicationStatus) throws ElectionException;

    /**
     * Respond to an election request.
     */
    VoteResponse handleElectionRequest(ElectionRequest electionRequest, ReplicationStatus replicationStatus);

}
