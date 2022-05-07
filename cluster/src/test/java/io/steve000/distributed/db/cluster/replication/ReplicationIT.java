package io.steve000.distributed.db.cluster.replication;

import com.sun.net.httpserver.HttpServer;
import io.steve000.distributed.db.cluster.Leader;
import io.steve000.distributed.db.cluster.replication.log.ReplicationLog;
import io.steve000.distributed.db.common.NetworkUtils;
import io.steve000.distributed.db.registry.api.RegistryEntry;
import io.steve000.distributed.db.registry.api.RegistryResponse;
import io.steve000.distributed.db.registry.client.RegistryClient;
import io.steve000.distributed.db.registry.client.RegistryException;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class ReplicationIT {

    @Test
    void testReplicatingData() throws ReplicationException, RegistryException, IOException, InterruptedException {
        RegistryClient registryClient = mock(RegistryClient.class);
        try (
                TestServer server1 = new TestServer(NetworkUtils.findOpenPort(), registryClient);
                TestServer server2 = new TestServer(NetworkUtils.findOpenPort(), registryClient);
                TestServer server3 = new TestServer(NetworkUtils.findOpenPort(), registryClient);
                TestServer server4 = new TestServer(NetworkUtils.findOpenPort(), registryClient)
        ) {
            when(registryClient.getRegistry()).thenReturn(new RegistryResponse(
                    Arrays.asList(
                            new RegistryEntry(server1.name, "0.0.0.0", server1.port),
                            new RegistryEntry(server2.name, "0.0.0.0", server2.port),
                            new RegistryEntry(server3.name, "0.0.0.0", server3.port),
                            new RegistryEntry(server4.name, "0.0.0.0", server4.port)
                    )
            ));

            Replicatable replicatable1 = new Replicatable("event", "something");
            Replicatable replicatable2 = new Replicatable("event2", "something else");
            Replicatable replicatable3 = new Replicatable("event", "something");
            server1.replicationService.replicate(replicatable1);
            server1.replicationService.replicate(replicatable2);
            server1.replicationService.replicate(replicatable3);

            List<Replicatable> expectedResults = List.of(replicatable1, replicatable2, replicatable3);
            assertEquals(expectedResults, drain(server1.replicationReceiver.receivedReplicatableQueue, 3));
            assertEquals(expectedResults, drain(server2.replicationReceiver.receivedReplicatableQueue, 3));
            assertEquals(expectedResults, drain(server3.replicationReceiver.receivedReplicatableQueue, 3));
            assertEquals(expectedResults, drain(server4.replicationReceiver.receivedReplicatableQueue, 3));
        }
    }

    @Test
    void testReplicationSync() throws IOException, RegistryException, ReplicationException, InterruptedException, ReplicationLogException {
        RegistryClient registryClient = mock(RegistryClient.class);
        try (
                TestServer server1 = new TestServer(NetworkUtils.findOpenPort(), registryClient);
                TestServer server2 = new TestServer(NetworkUtils.findOpenPort(), registryClient);
        ) {
            when(registryClient.getRegistry()).thenReturn(new RegistryResponse(
                    Arrays.asList(
                            //second server is not known to registry
                            new RegistryEntry(server1.name, "0.0.0.0", server1.port)
                    )
            ));

            Replicatable replicatable1 = new Replicatable("event", "something");
            Replicatable replicatable2 = new Replicatable("event2", "something else");
            Replicatable replicatable3 = new Replicatable("event", "something");
            server1.replicationService.replicate(replicatable1);
            server1.replicationService.replicate(replicatable2);
            server1.replicationService.replicate(replicatable3);

            List<Replicatable> expectedResults = List.of(replicatable1, replicatable2, replicatable3);
            assertEquals(expectedResults, drain(server1.replicationReceiver.receivedReplicatableQueue, 3));

            server2.replicationHandler.sync(new Leader("", false, "0.0.0.0", server1.port));
            assertEquals(expectedResults, drain(server2.replicationReceiver.receivedReplicatableQueue, 3));
        }
    }

    private static <T> List<T> drain(BlockingQueue<T> queue, int expectedElements) throws InterruptedException {
        List<T> results = new ArrayList<>();
        for (int i = 0; i < expectedElements; i++) {
            results.add(queue.poll(5, TimeUnit.SECONDS));
        }
        return results;
    }

    private static class TestServer implements Closeable {

        private final String name;

        private final int port;

        private final ReplicationService replicationService;

        private final ReplicationHandler replicationHandler;

        private final HttpServer httpServer;

        private final TestReplicationReceiver replicationReceiver;

        public TestServer(int port, RegistryClient registryClient) throws IOException {
            name = UUID.randomUUID().toString();
            this.port = port;
            replicationReceiver = new TestReplicationReceiver();
            this.replicationHandler = new ReplicationHandler(ReplicationLog.open(), replicationReceiver);
            replicationService = new SimpleReplicationService(replicationHandler, registryClient, name);
            httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            httpServer.createContext("/cluster/replication", new ReplicationHttpHandler(replicationHandler));
            httpServer.start();
        }

        @Override
        public void close() {
            httpServer.stop(0);
        }
    }

    private static class TestReplicationReceiver implements ReplicationReceiver {

        final BlockingQueue<Replicatable> receivedReplicatableQueue = new LinkedBlockingDeque<>(5);

        @Override
        public void receiveReplication(Replicatable replicatable) {
            receivedReplicatableQueue.offer(replicatable);
        }
    }

}
