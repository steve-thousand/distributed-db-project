package io.steve000.distributed.db.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DistributedDBServer {

    public static void main(String args[]) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);

        Executor executor = Executors.newFixedThreadPool(10);

        server.createContext("/", new DBHandler());
        server.setExecutor(executor);
        server.start();
    }

}
