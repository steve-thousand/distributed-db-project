package io.steve000.distributed.db.node.server.cluster.election;

import io.steve000.distributed.db.node.server.cluster.election.api.VoteResponse;

import java.util.List;

public class VoteResults {

    private final List<VoteResponse> voteResponses;

    public VoteResults(List<VoteResponse> voteResponses) {
        this.voteResponses = voteResponses;
    }

    public List<VoteResponse> getVoteResponses() {
        return voteResponses;
    }
}
