package io.steve000.distributed.db.node.server.cluster.election;

import io.steve000.distributed.db.node.server.cluster.Leader;
import io.steve000.distributed.db.node.server.cluster.ReplicationStatus;

import java.time.LocalDateTime;

public class SimpleElectionService implements ElectionService {

    private final ElectionCoordinator electionCoordinator;

    public SimpleElectionService(ElectionCoordinator electionCoordinator) {
        this.electionCoordinator = electionCoordinator;
    }

    @Override
    public Leader electLeader(ReplicationStatus replicationStatus) throws ElectionException {
        return electionCoordinator.runElection();
    }

    @Override
    public VoteResponse handleElectionRequest(ElectionRequest electionRequest, ReplicationStatus replicationStatus) {
        //ignore foreign election requests if this node has already started an earlier election
        LocalDateTime thisElectionStartTime = electionCoordinator.getElectionStartTime();
        if (thisElectionStartTime != null &&
                thisElectionStartTime.isBefore(electionRequest.getElectionStartTime())) {
            return null;
        }
        return new VoteResponse(replicationStatus.getGeneration(), replicationStatus.getName(), LocalDateTime.now());
    }
}
