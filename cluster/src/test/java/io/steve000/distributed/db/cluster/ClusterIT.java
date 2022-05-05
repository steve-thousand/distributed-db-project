package io.steve000.distributed.db.cluster;

import com.sun.net.httpserver.HttpServer;
import io.steve000.distributed.db.cluster.election.Elector;
import io.steve000.distributed.db.cluster.election.bully.BullyElector;
import io.steve000.distributed.db.cluster.http.ClusterHttpClient;
import io.steve000.distributed.db.common.NetworkUtils;
import io.steve000.distributed.db.registry.client.RegistryClient;
import io.steve000.distributed.db.registry.server.DistributedDBRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ClusterIT {

    private static final Logger logger = LoggerFactory.getLogger(ClusterIT.class);

    private DistributedDBRegistry registryServer;

    private String registryHost;

    @BeforeEach
    void beforeEach() throws IOException {
        InetSocketAddress address = new InetSocketAddress("0.0.0.0", NetworkUtils.findOpenPort());
        HttpServer server = HttpServer.create(address, 0);
        registryServer = new DistributedDBRegistry(server);
        registryServer.run();
        registryHost = "http://" + address.getHostString() + ":" + address.getPort();
    }

    @AfterEach
    void afterEach() {
        registryServer.close();
    }

    /**
     * Testing a single node registering and electing itself
     */
    @Test
    void testOneServerElectsSelf() throws IOException {
        try(TestServer server1 = new TestServer()) {

            //test that server1 elects itself leader
            server1.register();

            assertEquals(server1.getName(), server1.getLeader().getName());
        }
    }

    @Test
    void testNewServersUseExistingLeader() throws IOException {
        try (
                TestServer server1 = new TestServer("test-server-1");
                TestServer server2 = new TestServer("test-server-2");
                TestServer server3 = new TestServer("test-server-3")
        ) {
            //test that server1 elects itself leader
            server1.register();
            assertEquals(server1.getName(), server1.getLeader().getName());

            //test that server2 uses server1 as leader
            server2.register();
            assertEquals(server1.getName(), server1.getLeader().getName());
            assertEquals(server1.getName(), server2.getLeader().getName());

            //test that server3 uses server1 as leader
            server3.register();
            assertEquals(server1.getName(), server1.getLeader().getName());
            assertEquals(server1.getName(), server2.getLeader().getName());
            assertEquals(server1.getName(), server3.getLeader().getName());
        }
    }

    @Test
    void testElectionOccursAfterLeaderDeath() throws Exception {
        try (
                TestServer server1 = new TestServer("test-server-1");
                TestServer server2 = new TestServer("test-server-2")
        ) {
            //test that server1 elects itself leader
            server1.register();
            assertEquals(server1.getName(), server1.getLeader().getName());

            //test that server2 uses server1 as leader
            server2.register();
            assertEquals(server1.getName(), server1.getLeader().getName());
            assertEquals(server1.getName(), server2.getLeader().getName());

            //kill leader
            server1.kill();

            Thread.sleep(500);
            assertEquals(server2.getName(), server2.getLeader().getName());
        }
    }

    @Test
    void testElectionOccursAfterLeaderDeath_multipleFollowers() throws Exception {
        try (
                TestServer server1 = new TestServer("test-server-1");
                TestServer server2 = new TestServer("test-server-2");
                TestServer server3 = new TestServer("test-server-3")
        ) {
            //test that server1 elects itself leader
            server1.register();
            assertEquals(server1.getName(), server1.getLeader().getName());

            //test that server2 uses server1 as leader
            server2.register();
            assertEquals(server1.getName(), server1.getLeader().getName());
            assertEquals(server1.getName(), server2.getLeader().getName());

            //test that server3 uses server1 as leader
            server3.register();
            assertEquals(server1.getName(), server1.getLeader().getName());
            assertEquals(server1.getName(), server2.getLeader().getName());
            assertEquals(server1.getName(), server3.getLeader().getName());

            //kill leader
            logger.info("Killing server 1...");
            server1.kill();

            Thread.sleep(500);

            //right now can't easily assert which one will be the new leader
            logger.info("Getting second server's leader...");
            String leader2 = server2.getLeader().getName();
            logger.info("Getting third server's leader...");
            String leader3 = server3.getLeader().getName();
            assertNotEquals(server1.getName(), leader2);
            assertEquals(leader2, leader3);
        }
    }

    private class TestServer implements AutoCloseable {

        private final String name;

        private final ClusterService clusterService;

        private final HttpServer httpServer;

        public TestServer() throws IOException {
            this(UUID.randomUUID().toString());
        }

        public TestServer(String name) throws IOException {
            this(name, NetworkUtils.findOpenPort());
        }

        public TestServer(String name, int port) throws IOException {
            this.name = name;

            ClusterConfig config = new ClusterConfig();
            config.setCusterThreadPeriodMs(50);
            config.setName(name);

            RegistryClient registryClient = new RegistryClient(registryHost, name, port);
            ClusterHttpClient clusterHttpClient = new ClusterHttpClient(name, config.getCusterThreadPeriodMs());
            httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

            Elector elector = new BullyElector(registryClient, httpServer);

            clusterService = new SimpleClusterService.Builder()
                    .withConfig(config)
                    .withElector(elector)
                    .withRegistryClient(registryClient)
                    .withClusterHttpClient(clusterHttpClient)
                    .build();

            new HttpServerCluster(httpServer, clusterService);
            httpServer.setExecutor(Executors.newFixedThreadPool(2));
            httpServer.start();
        }

        public void register() {
            clusterService.run();
        }

        public void kill() throws Exception {
            httpServer.stop(0);
            clusterService.close();
        }

        public String getName() {
            return name;
        }

        public Leader getLeader() {
            return clusterService.getLeader();
        }

        @Override
        public void close() {
            try {
                kill();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
