package io.steve000.distributed.db.cluster.replication;

import com.sun.net.httpserver.HttpServer;
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
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class ReplicationIT {

    @Test
    void testReplicatingData() throws ReplicationException, RegistryException, IOException, InterruptedException {
        try (
                TestServer server1 = new TestServer();
                TestServer server2 = new TestServer();
                TestServer server3 = new TestServer()
        ) {
            TestReplicationReceiver replicationReceiver = new TestReplicationReceiver();
            ReplicationHandler replicationHandler = new ReplicationHandler(ReplicationLog.open(), replicationReceiver);

            RegistryClient registryClient = mock(RegistryClient.class);
            when(registryClient.getRegistry()).thenReturn(new RegistryResponse(
                    Arrays.asList(
                            new RegistryEntry("self", "", 8079),
                            new RegistryEntry("", "0.0.0.0", server1.port),
                            new RegistryEntry("", "0.0.0.0", server2.port),
                            new RegistryEntry("", "0.0.0.0", server3.port)
                    )
            ));

            ReplicationService replicationService = new SimpleReplicationService(replicationHandler, registryClient, "self");

            Replicatable replicatable = new Replicatable("event", "something");
            replicationService.replicate(replicatable);

            assertEquals(replicatable, replicationReceiver.receivedReplicatableQueue.poll(5, TimeUnit.SECONDS));
            assertEquals(replicatable, server1.replicationReceiver.receivedReplicatableQueue.poll(5, TimeUnit.SECONDS));
            assertEquals(replicatable, server2.replicationReceiver.receivedReplicatableQueue.poll(5, TimeUnit.SECONDS));
            assertEquals(replicatable, server3.replicationReceiver.receivedReplicatableQueue.poll(5, TimeUnit.SECONDS));

            replicatable = new Replicatable("event2", "something else");
            replicationService.replicate(replicatable);

            assertEquals(replicatable, replicationReceiver.receivedReplicatableQueue.poll(5, TimeUnit.SECONDS));
            assertEquals(replicatable, server1.replicationReceiver.receivedReplicatableQueue.poll(5, TimeUnit.SECONDS));
            assertEquals(replicatable, server2.replicationReceiver.receivedReplicatableQueue.poll(5, TimeUnit.SECONDS));
            assertEquals(replicatable, server3.replicationReceiver.receivedReplicatableQueue.poll(5, TimeUnit.SECONDS));
        }
    }

    private static class TestServer implements Closeable {

        private final int port;

        private final ReplicationHandler replicationHandler;

        private final HttpServer httpServer;

        private final TestReplicationReceiver replicationReceiver;

        public TestServer() throws IOException {
            port = NetworkUtils.findOpenPort();
            replicationReceiver = new TestReplicationReceiver();
            this.replicationHandler = new ReplicationHandler(ReplicationLog.open(), replicationReceiver);
            spy(this.replicationHandler);
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
