package io.steve000.distributed.db.node.server.http;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.steve000.distributed.db.cluster.replication.Replicatable;
import io.steve000.distributed.db.cluster.replication.ReplicationException;
import io.steve000.distributed.db.cluster.replication.ReplicationService;
import io.steve000.distributed.db.node.server.db.DBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DBHttpHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(DBHttpHandler.class);

    private final DBService dbService;

    private final ReplicationService replicationService;

    public DBHttpHandler(DBService dbService, ReplicationService replicationService) {
        this.dbService = dbService;
        this.replicationService = replicationService;
    }

    public void handle(HttpExchange httpExchange) throws IOException {
        if ("GET".equals(httpExchange.getRequestMethod())) {
            final String key = getKeyFromUri(httpExchange);
            final String value = dbService.get(key);
            if (value == null) {
                httpExchange.sendResponseHeaders(404, 0);
            } else {
                byte[] charArray = value.getBytes();
                httpExchange.sendResponseHeaders(200, charArray.length);
                httpExchange.getResponseBody().write(charArray, 0, charArray.length);
            }
        } else if ("POST".equals(httpExchange.getRequestMethod())) {
            //if we are not the leader, forward to leader
            //TODO do it

            //if we are the leader, this is a write action, but will require replication to followers
            final String key = getKeyFromUri(httpExchange);
            final String value = getValueFromBody(httpExchange);

            try {
                replicationService.replicate(new Replicatable(key, value));
            }catch(ReplicationException e) {
                logger.error("Error encountered in replication", e);
                httpExchange.sendResponseHeaders(500, 0);
            }

            httpExchange.sendResponseHeaders(201, 0);
        }
        httpExchange.getResponseBody().flush();
        httpExchange.getResponseBody().close();
    }

    static String getKeyFromUri(HttpExchange httpExchange) {
        HttpContext context = httpExchange.getHttpContext();
        return httpExchange.getRequestURI().getPath().replace(context.getPath(),"").substring(1);
    }

    static String getValueFromBody(HttpExchange httpExchange) throws IOException {
        int length = Integer.parseInt(httpExchange.getRequestHeaders().get("Content-Length").get(0));
        byte[] postArray = new byte[length];
        httpExchange.getRequestBody().read(postArray, 0, length);
        return new String(postArray);
    }
}
