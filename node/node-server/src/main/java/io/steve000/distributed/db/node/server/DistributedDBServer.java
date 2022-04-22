package io.steve000.distributed.db.node.server;

import com.sun.net.httpserver.HttpServer;
import io.steve000.distributed.db.node.server.cluster.ReplicationStatus;
import io.steve000.distributed.db.node.server.cluster.election.ElectionClient;
import io.steve000.distributed.db.node.server.cluster.election.ElectionCoordinator;
import io.steve000.distributed.db.node.server.cluster.election.ElectionService;
import io.steve000.distributed.db.node.server.cluster.election.SimpleElectionService;
import io.steve000.distributed.db.node.server.http.ElectionHttpHandler;
import io.steve000.distributed.db.registry.api.RegistryResponse;
import io.steve000.distributed.db.registry.client.RegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DistributedDBServer {

    private static final Logger logger = LoggerFactory.getLogger(DistributedDBServer.class);

    public static void main(String args[]) throws IOException, InterruptedException {
        DBArgs dbArgs = CommandLine.populateCommand(new DBArgs(), args);

        final String name = UUID.randomUUID().toString();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        Executor executor = Executors.newFixedThreadPool(10);

        ReplicationStatus replicationStatus = new ReplicationStatus(name, 0);

        RegistryClient registryClient = new RegistryClient(dbArgs.registryAddress);
        ElectionService electionService = new SimpleElectionService(
                new ElectionCoordinator(registryClient, replicationStatus, new ElectionClient())
        );

        server.createContext("/db", new DBHandler());
        server.createContext("/elect", new ElectionHttpHandler(electionService, replicationStatus));
        server.setExecutor(executor);
        server.start();

        logger.info("Started DB server.");

        registryClient.register(name, dbArgs.adminPort);

        while(true) {
            RegistryResponse response = registryClient.getRegistry();
            logger.info("Received registry: {}", response);
            Thread.sleep(10000);
        }
    }

}
