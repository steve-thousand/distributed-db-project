package io.steve000.distributed.db.node.server;

import com.sun.net.httpserver.HttpServer;
import io.steve000.distributed.db.cluster.ClusterConfig;
import io.steve000.distributed.db.cluster.ClusterService;
import io.steve000.distributed.db.cluster.SimpleClusterService;
import io.steve000.distributed.db.cluster.election.bully.BullyElector;
import io.steve000.distributed.db.cluster.http.ClusterHttpClient;
import io.steve000.distributed.db.cluster.replication.ReplicationHandler;
import io.steve000.distributed.db.node.server.db.DBInMemoryService;
import io.steve000.distributed.db.node.server.db.DBService;
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
        ClusterConfig config = new ClusterConfig();
        config.setName(name);
        RegistryClient registryClient = new RegistryClient(dbArgs.registryAddress, name, dbArgs.adminPort);
        ClusterHttpClient clusterHttpClient = new ClusterHttpClient(name, config.getCusterThreadPeriodMs());

        DBService dbService = new DBInMemoryService();
        ReplicationHandler replicationHandler = new DBReplicationHandler(dbService);

        ClusterService clusterService = new SimpleClusterService.Builder()
                .withConfig(config)
                .withElector(new BullyElector(registryClient, server))
                .withRegistryClient(registryClient)
                .withClusterHttpClient(clusterHttpClient)
                .withReplicationHandler(replicationHandler)
                .build();

        clusterService.bind(server);

        server.createContext("/db", new DBHttpHandler(dbService, clusterService.replicationService()));
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        logger.info("Started DB server.");
        clusterService.run();
    }

}
