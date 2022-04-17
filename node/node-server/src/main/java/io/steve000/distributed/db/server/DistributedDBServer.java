package io.steve000.distributed.db.server;

import com.sun.net.httpserver.HttpServer;
import io.steve000.distributed.db.registry.client.RegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DistributedDBServer {

    private static final Logger logger = LoggerFactory.getLogger(DistributedDBServer.class);

    public static void main(String args[]) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);

        Executor executor = Executors.newFixedThreadPool(10);

        server.createContext("/", new DBHandler());
        server.setExecutor(executor);
        server.start();

        logger.info("Started DB server.");

        RegistryClient registryClient = new RegistryClient("http://localhost:8050");
        registryClient.register(8060);
    }

}
