package io.steve000.distributed.db.registry.server;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DistributedDBRegistry {

    private static final Logger logger = LoggerFactory.getLogger(DistributedDBRegistry.class);

    public static void main(String args[]) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        Executor executor = Executors.newFixedThreadPool(10);

        server.createContext("/", new RegistryHttpHandler(new InMemoryRegistry()));
        server.setExecutor(executor);
        server.start();

        logger.info("Started registry server.");
    }
}
