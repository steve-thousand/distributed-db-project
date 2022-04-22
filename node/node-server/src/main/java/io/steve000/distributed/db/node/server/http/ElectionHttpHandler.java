package io.steve000.distributed.db.node.server.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.steve000.distributed.db.node.server.cluster.ReplicationStatus;
import io.steve000.distributed.db.node.server.cluster.election.ElectionRequest;
import io.steve000.distributed.db.node.server.cluster.election.ElectionService;
import io.steve000.distributed.db.node.server.cluster.election.VoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ElectionHttpHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(ElectionHttpHandler.class);

    private final ElectionService electionService;

    private final ReplicationStatus replicationStatus;

    static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    public ElectionHttpHandler(ElectionService electionService, ReplicationStatus replicationStatus) {
        this.electionService = electionService;
        this.replicationStatus = replicationStatus;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("POST".equals(exchange.getRequestMethod())) {
                ElectionRequest electionRequest = objectMapper.readValue(exchange.getRequestBody(), ElectionRequest.class);
                VoteResponse voteResponse = electionService.handleElectionRequest(electionRequest, replicationStatus);

                if(voteResponse == null) {
                    //null response == refusal to vote because we think we are leading the election
                    exchange.sendResponseHeaders(204, 0);
                    exchange.getResponseBody().close();
                    return;
                }

                String responseJson = objectMapper.writeValueAsString(voteResponse);
                exchange.sendResponseHeaders(200, responseJson.length());
                exchange.getResponseBody().write(responseJson.getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().close();
            }
        }catch(Exception e) {
            logger.error("Uncaught exception", e);
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().close();
        }
    }
}
