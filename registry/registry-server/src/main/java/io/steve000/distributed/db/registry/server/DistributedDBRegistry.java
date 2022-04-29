package io.steve000.distributed.db.registry.server;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DistributedDBRegistry implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(DistributedDBRegistry.class);

    private final HttpServer httpServer;

    public DistributedDBRegistry(HttpServer httpServer) {
        this.httpServer = httpServer;
    }

    public static void main(String args[]) throws IOException {
        DistributedDBRegistry server = new DistributedDBRegistry(HttpServer.create(new InetSocketAddress(8080), 0));
        server.run();
    }

    public void run() {
        Executor executor = Executors.newFixedThreadPool(10);

        httpServer.createContext("/", new RegistryHttpHandler(new InMemoryRegistry()));
        httpServer.setExecutor(executor);
        httpServer.start();

        logger.info("Started registry server.");
    }

    @Override
    public void close() {
        httpServer.stop(0);
    }
}
