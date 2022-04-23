package io.steve000.distributed.db.node.server.cluster.election.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

public class VoteResponse {

    private final int generation;

    private final String name;

    private final LocalDateTime voteTime;

    @JsonCreator
    public VoteResponse(
            @JsonProperty("generation") int generation,
            @JsonProperty("name") String name,
            @JsonProperty("voteTime") LocalDateTime voteTime
    ) {
        this.generation = generation;
        this.name = name;
        this.voteTime = voteTime;
    }

    public int getGeneration() {
        return generation;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getVoteTime() {
        return voteTime;
    }

    @Override
    public String toString() {
        return "VoteResponse{" +
                "generation=" + generation +
                ", name='" + name + '\'' +
                ", voteTime=" + voteTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VoteResponse that = (VoteResponse) o;
        return generation == that.generation && Objects.equals(name, that.name) && Objects.equals(voteTime, that.voteTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(generation, name, voteTime);
    }
}
