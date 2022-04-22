package io.steve000.distributed.db.node.server.cluster;

import io.steve000.distributed.db.node.server.cluster.election.ElectionException;
import io.steve000.distributed.db.node.server.cluster.election.ElectionService;

public class SimpleClusterService implements ClusterService {

    private final ElectionService electionService;

    private final ReplicationStatus replicationStatus;

    private Leader leader;

    public SimpleClusterService(ElectionService electionService, String name) {
        this.electionService = electionService;
        this.replicationStatus = new ReplicationStatus(name, 0);
    }

    @Override
    public Leader getLeader() {
        try {
            //do we have leader?
            if (leader == null || !isLeaderLive()) {
                this.leader = electionService.electLeader(replicationStatus);
            }

            return leader;
        }catch(ElectionException e) {
            throw new RuntimeException("Failed to get leader", e);
        }
    }

    private boolean isLeaderLive() {
        //TODO fix it
        return true;
    }
}
