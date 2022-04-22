package io.steve000.distributed.db.node.server.cluster.election;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class ElectionRequest {

    private final LocalDateTime electionStartTime;

    @JsonCreator
    public ElectionRequest(@JsonProperty("electionStartTime") LocalDateTime electionStartTime) {
        this.electionStartTime = electionStartTime;
    }

    public LocalDateTime getElectionStartTime() {
        return electionStartTime;
    }
}
