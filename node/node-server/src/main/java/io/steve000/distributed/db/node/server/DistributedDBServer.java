package io.steve000.distributed.db.node.server;

import com.sun.net.httpserver.HttpServer;
import io.steve000.distributed.db.cluster.ClusterService;
import io.steve000.distributed.db.cluster.SimpleClusterService;
import io.steve000.distributed.db.cluster.election.bully.BullyElectionService;
import io.steve000.distributed.db.node.server.http.DBHttpHandler;
import io.steve000.distributed.db.registry.client.RegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Executors;

public class DistributedDBServer {

    private static final Logger logger = LoggerFactory.getLogger(DistributedDBServer.class);

    public static void main(String args[]) throws IOException {
        DBArgs dbArgs = CommandLine.populateCommand(new DBArgs(), args);
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        final String name = UUID.randomUUID().toString();
        RegistryClient registryClient = new RegistryClient(dbArgs.registryAddress);
        ClusterService clusterService = new SimpleClusterService(new BullyElectionService(registryClient, server), registryClient, name);

        clusterService.bind(server);

        server.createContext("/db", new DBHttpHandler());
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        logger.info("Started DB server.");
        clusterService.register(name, dbArgs.adminPort);
    }

}
