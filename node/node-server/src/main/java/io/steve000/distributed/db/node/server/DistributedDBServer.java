package io.steve000.distributed.db.node.server;

import com.sun.net.httpserver.HttpServer;
import io.steve000.distributed.db.node.server.cluster.ClusterService;
import io.steve000.distributed.db.node.server.cluster.SimpleClusterService;
import io.steve000.distributed.db.node.server.cluster.election.ElectionClient;
import io.steve000.distributed.db.node.server.cluster.election.ElectionCoordinator;
import io.steve000.distributed.db.node.server.cluster.election.ElectionService;
import io.steve000.distributed.db.node.server.cluster.election.SimpleElectionService;
import io.steve000.distributed.db.node.server.http.DBHttpHandler;
import io.steve000.distributed.db.node.server.http.ClusterHttpHandler;
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

        DistributedDBResources resources = getResources(dbArgs);

        server.createContext("/db", new DBHttpHandler());
        server.createContext("/cluster", new ClusterHttpHandler(resources.clusterService));
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        logger.info("Started DB server.");

        resources.clusterService.register(resources.name, dbArgs.adminPort);
    }

    private static DistributedDBResources getResources(DBArgs dbArgs) {
        final String name = UUID.randomUUID().toString();

        RegistryClient registryClient = new RegistryClient(dbArgs.registryAddress);

        ElectionService electionService = new SimpleElectionService(
                new ElectionCoordinator(registryClient, new ElectionClient())
        );

        ClusterService clusterService = new SimpleClusterService(electionService, registryClient, name);

        return new DistributedDBResources(
                name,
                clusterService
        );
    }

    private static class DistributedDBResources {

        private final String name;

        private final ClusterService clusterService;

        public DistributedDBResources(String name, ClusterService clusterService) {
            this.name = name;
            this.clusterService = clusterService;
        }
    }

}
