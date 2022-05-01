package io.steve000.distributed.db.cluster.replication;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.steve000.distributed.db.common.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ReplicationHttpHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(ReplicationHttpHandler.class);

    private final ReplicationHandler replicationHandler;

    public ReplicationHttpHandler(ReplicationHandler replicationHandler) {
        this.replicationHandler = replicationHandler;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            //clearly I am getting lazy about my http layers
            Replicatable replicatable = JSON.OBJECT_MAPPER.readValue(exchange.getRequestBody(), Replicatable.class);
            replicationHandler.handleReplication(replicatable);
            exchange.sendResponseHeaders(202, 0);
        }catch(Exception e) {
            exchange.sendResponseHeaders(500, 0);
        }
    }
}
