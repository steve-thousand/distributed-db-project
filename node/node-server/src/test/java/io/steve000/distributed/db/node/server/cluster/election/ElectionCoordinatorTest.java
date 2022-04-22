package io.steve000.distributed.db.node.server.cluster.election;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ElectionCoordinatorTest {

    private static Stream<Arguments> handleVotesTestParams() {
        return Stream.of(
                Arguments.of(Collections.emptyList(), 1, LocalDateTime.now(), null),

                //one higher generation response
                Arguments.of(Collections.singletonList(
                                new VoteResponse(2, "test", LocalDateTime.of(2022, 4, 18, 12, 12, 12))
                        ), 1, LocalDateTime.of(2022, 4, 18, 12, 12, 11),
                        new VoteResponse(2, "test", LocalDateTime.of(2022, 4, 18, 12, 12, 12))
                ),

                //two higher generation response, expected earliest vote
                Arguments.of(Arrays.asList(
                                new VoteResponse(2, "test", LocalDateTime.of(2022, 4, 18, 12, 12, 12)),
                                new VoteResponse(2, "test2", LocalDateTime.of(2022, 4, 18, 12, 12, 11))
                        ), 1, LocalDateTime.of(2022, 4, 18, 12, 12, 11),
                        new VoteResponse(2, "test2", LocalDateTime.of(2022, 4, 18, 12, 12, 11))
                ),

                //one lower generation response
                Arguments.of(Collections.singletonList(
                                new VoteResponse(1, "test", LocalDateTime.of(2022, 4, 18, 12, 12, 12))
                        ), 2, LocalDateTime.of(2022, 4, 18, 12, 12, 11),
                        null
                ),

                //two responses, one higher generation
                Arguments.of(Arrays.asList(
                                new VoteResponse(1, "test", LocalDateTime.of(2022, 4, 18, 12, 12, 12)),
                                new VoteResponse(3, "test2", LocalDateTime.of(2022, 4, 18, 12, 12, 12))
                        ), 2, LocalDateTime.of(2022, 4, 18, 12, 12, 11),
                        new VoteResponse(3, "test2", LocalDateTime.of(2022, 4, 18, 12, 12, 12))
                )
        );
    }

    @ParameterizedTest
    @MethodSource("handleVotesTestParams")
    void handleVotes_parameterizedTest(List<VoteResponse> responses, int generation, LocalDateTime time, VoteResponse expected) {
        VoteResults voteResults = new VoteResults(responses);
        VoteResponse voteResponse = ElectionCoordinator.handleVotes(voteResults, generation, time);
        assertEquals(expected, voteResponse);
    }

}
