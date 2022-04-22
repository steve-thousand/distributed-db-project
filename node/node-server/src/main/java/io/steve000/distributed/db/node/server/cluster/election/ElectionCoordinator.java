package io.steve000.distributed.db.node.server.cluster.election;

import io.steve000.distributed.db.node.server.cluster.Leader;
import io.steve000.distributed.db.node.server.cluster.ReplicationStatus;
import io.steve000.distributed.db.registry.api.RegistryEntry;
import io.steve000.distributed.db.registry.api.RegistryResponse;
import io.steve000.distributed.db.registry.client.RegistryClient;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Runs and completes a full election.
 */
public class ElectionCoordinator {

    private final RegistryClient registryClient;

    private final ReplicationStatus replicationStatus;

    private final ElectionClient electionClient;

    private LocalDateTime electionStartTime;

    public ElectionCoordinator(RegistryClient registryClient, ReplicationStatus replicationStatus, ElectionClient electionClient) {
        this.registryClient = registryClient;
        this.replicationStatus = replicationStatus;
        this.electionClient = electionClient;
    }

    public LocalDateTime getElectionStartTime() {
        return electionStartTime;
    }

    public Leader runElection() throws ElectionException {
        try {
            electionStartTime = LocalDateTime.now();
            int generation = replicationStatus.getGeneration();

            RegistryResponse response = registryClient.getRegistry();
            List<RegistryEntry> registryEntries = response.getRegistryEntries();
            VoteResults voteResults = electionClient.sendElectionRequests(registryEntries, electionStartTime);

            VoteResponse winningVote = handleVotes(voteResults, generation, electionStartTime);

            final Leader leader;
            if (winningVote == null) {
                leader = new Leader(replicationStatus.getName(), true);
            } else {
                leader = new Leader(winningVote.getName(), false);
            }

            return leader;
        } catch (Throwable e) {
            throw new ElectionException(e);
        } finally {
            electionStartTime = null;
        }
    }

    static VoteResponse handleVotes(VoteResults voteResults, int thisGeneration, LocalDateTime thisVoteTime) {
        List<VoteResponse> orderedByGenerationThenTime = voteResults.getVoteResponses().stream()
                .sorted(
                        Comparator.comparing(VoteResponse::getVoteTime)
                                .thenComparing(VoteResponse::getGeneration, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        VoteResponse winningVote = null;
        if (orderedByGenerationThenTime.size() > 0) {
            if (orderedByGenerationThenTime.get(0).getGeneration() > thisGeneration) {
                winningVote = orderedByGenerationThenTime.get(0);
            } else {
                Iterator<VoteResponse> voteIterator = orderedByGenerationThenTime.listIterator();
                while (voteIterator.hasNext()) {
                    VoteResponse next = voteIterator.next();
                    if (next.getGeneration() == thisGeneration && next.getVoteTime().isBefore(thisVoteTime)) {
                        winningVote = next;
                        break;
                    }
                }
            }
        }

        return winningVote;
    }

}
