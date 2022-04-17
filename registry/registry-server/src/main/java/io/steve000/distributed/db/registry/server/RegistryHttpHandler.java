package io.steve000.distributed.db.registry.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.steve000.distributed.db.registry.api.RegisterRequest;
import io.steve000.distributed.db.registry.api.RegistryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

public class RegistryHttpHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(RegistryHttpHandler.class);

    private final Registry registry;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public RegistryHttpHandler(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        logger.info("Request {} {}", httpExchange.getRequestMethod(), httpExchange.getRequestURI());
        try {
            if ("GET".equals(httpExchange.getRequestMethod())) {
                List<RegistryEntry> records = registry.getRecords();
                byte[] charArray = objectMapper.writeValueAsString(records).getBytes();
                httpExchange.sendResponseHeaders(200, charArray.length);
                httpExchange.getResponseBody().write(charArray, 0, charArray.length);
            } else if ("POST".equals(httpExchange.getRequestMethod())) {
                InetSocketAddress address = httpExchange.getRemoteAddress();
                String requestBody = new String(httpExchange.getRequestBody().readAllBytes());
                RegisterRequest request = objectMapper.readValue(requestBody, RegisterRequest.class);
                RegistryEntry entry = new RegistryEntry(address.getHostString(), request.getPort());
                registry.register(entry);
                httpExchange.sendResponseHeaders(201, 0);
            }
        }catch(Exception e) {
            logger.error("Exception processing request {}", httpExchange.getRequestURI(), e);
            httpExchange.sendResponseHeaders(500, 0);
        }
        httpExchange.getResponseBody().flush();
        httpExchange.getResponseBody().close();
    }
}
