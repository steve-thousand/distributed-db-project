package io.steve000.distributed.db.node.server.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.steve000.distributed.db.node.server.JSON;
import io.steve000.distributed.db.node.server.cluster.ClusterService;
import io.steve000.distributed.db.node.server.cluster.election.api.ElectionRequest;
import io.steve000.distributed.db.node.server.cluster.election.api.VoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ClusterHttpHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(ClusterHttpHandler.class);

    private final ClusterService clusterService;

    public ClusterHttpHandler(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            final String handlerPath = exchange.getRequestURI().getPath()
                    .replace(exchange.getHttpContext().getPath(), "");
            if(handlerPath.equals("/heartbeat")){
                heartBeat(exchange);
                return;
            }
            if(handlerPath.equals("/election")){
                election(exchange);
                return;
            }
            exchange.sendResponseHeaders(404, 0);
        }catch(Exception e) {
            logger.error("Uncaught exception", e);
            exchange.sendResponseHeaders(500, 0);
        }finally {
            exchange.getRequestBody().close();
            exchange.getResponseBody().close();
        }
    }

    private void heartBeat(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            //handle heartbeat from leader
            HeartBeat heartBeat = JSON.OBJECT_MAPPER.readValue(exchange.getRequestBody(), HeartBeat.class);
            clusterService.handleHeartBeat(heartBeat);
            exchange.sendResponseHeaders(201, 0);
        } else {
            exchange.sendResponseHeaders(405, 0);
        }
    }

    private void election(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            ElectionRequest electionRequest = JSON.OBJECT_MAPPER.readValue(
                    exchange.getRequestBody(), ElectionRequest.class);
            VoteResponse voteResponse = clusterService.handleElectionRequest(electionRequest);
            if (voteResponse == null) {
                //null response == refusal to vote because we think we are leading the election
                exchange.sendResponseHeaders(204, 0);
                return;
            }

            String responseJson = JSON.OBJECT_MAPPER.writeValueAsString(voteResponse);
            exchange.sendResponseHeaders(200, responseJson.length());
            exchange.getResponseBody().write(responseJson.getBytes(StandardCharsets.UTF_8));
        } else {
            exchange.sendResponseHeaders(405, 0);
        }
    }
}
