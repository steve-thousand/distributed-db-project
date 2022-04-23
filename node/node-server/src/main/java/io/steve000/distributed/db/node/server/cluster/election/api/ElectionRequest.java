package io.steve000.distributed.db.node.server.cluster.election.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

public class ElectionRequest {

    private final LocalDateTime electionStartTime;

    @JsonCreator
    public ElectionRequest(@JsonProperty("electionStartTime") LocalDateTime electionStartTime) {
        this.electionStartTime = electionStartTime;
    }

    public LocalDateTime getElectionStartTime() {
        return electionStartTime;
    }

    @Override
    public String toString() {
        return "ElectionRequest{" +
                "electionStartTime=" + electionStartTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElectionRequest that = (ElectionRequest) o;
        return Objects.equals(electionStartTime, that.electionStartTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(electionStartTime);
    }
}
