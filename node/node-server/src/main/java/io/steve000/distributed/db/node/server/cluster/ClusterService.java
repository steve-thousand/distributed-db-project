package io.steve000.distributed.db.node.server.cluster;

import io.steve000.distributed.db.node.server.cluster.election.api.ElectionRequest;
import io.steve000.distributed.db.node.server.cluster.election.api.VoteResponse;
import io.steve000.distributed.db.node.server.http.HeartBeat;

/**
 * I LIKE INTERFACES EVEN IF THEY ONLY HAVE ONE IMPLEMENTATION BECAUSE THEY ENFORCE A LAYER OF SEPARATION, OK?
 */
public interface ClusterService {

    Leader getLeader();

    void register(String name, int adminPort);

    void handleHeartBeat(HeartBeat heartBeat);

    void sendHeartBeats();

    VoteResponse handleElectionRequest(ElectionRequest electionRequest);

}
