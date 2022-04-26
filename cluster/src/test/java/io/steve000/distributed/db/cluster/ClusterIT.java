package io.steve000.distributed.db.cluster;

import com.sun.net.httpserver.HttpServer;
import io.steve000.distributed.db.cluster.election.ElectionService;
import io.steve000.distributed.db.cluster.election.bully.BullyElectionService;
import io.steve000.distributed.db.common.NetworkUtils;
import io.steve000.distributed.db.registry.client.RegistryClient;
import io.steve000.distributed.db.registry.server.DistributedDBRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClusterIT {

    private DistributedDBRegistry registryServer;

    private RegistryClient registryClient;

    @BeforeEach
    void beforeEach() throws IOException {
        InetSocketAddress address = new InetSocketAddress("0.0.0.0", NetworkUtils.findOpenPort());
        HttpServer server = HttpServer.create(address, 0);
        registryServer = new DistributedDBRegistry(server);
        registryServer.run();
        String registryHost = "http://" + address.getHostString() + ":" + address.getPort();
        registryClient = new RegistryClient(registryHost);
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
        TestServer server1 = new TestServer(registryClient);

        //test that server1 elects itself leader
        server1.register();

        assertEquals(server1.getName(), server1.getLeader().getName());
    }

    @Test
    void testNewServersUseExistingLeader() throws IOException {
        TestServer server1 = new TestServer(registryClient);
        TestServer server2 = new TestServer(registryClient);
        TestServer server3 = new TestServer(registryClient);

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

    @Test
    void testNewLeaderElectedAfterLeaderDeath() throws IOException {
        TestServer server1 = new TestServer(registryClient);
        TestServer server2 = new TestServer(registryClient);

        //test that server1 elects itself leader
        server1.register();
        assertEquals(server1.getName(), server1.getLeader().getName());

        //test that server2 uses server1 as leader
        server2.register();
        assertEquals(server1.getName(), server2.getLeader().getName());

        server1.kill();

        //server2 should now be leader
        assertEquals(server2.getName(), server2.getLeader().getName());
    }

    private static class TestServer {

        private final String name;

        private final int port;

        private final ClusterService clusterService;

        private final HttpServer httpServer;

        public TestServer(RegistryClient registryClient) throws IOException {
            this(UUID.randomUUID().toString(), registryClient);
        }

        public TestServer(String name, RegistryClient registryClient) throws IOException {
            this(NetworkUtils.findOpenPort(), name, registryClient);
        }

        public TestServer(int port, String name, RegistryClient registryClient) throws IOException {
            this.port = port;
            this.name = name;
            httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

            ElectionService electionService = new BullyElectionService(registryClient, httpServer);

            clusterService = new SimpleClusterService(electionService, registryClient, name);
            new HttpServerCluster(httpServer, clusterService);
            httpServer.setExecutor(Executors.newFixedThreadPool(2));
            httpServer.start();
        }

        public void register() {
            clusterService.register(name, port);
        }

        public void kill() {
            clusterService.unregister();
            httpServer.stop(0);
        }

        public String getName() {
            return name;
        }

        public Leader getLeader() {
            return clusterService.getLeader();
        }
    }
}
