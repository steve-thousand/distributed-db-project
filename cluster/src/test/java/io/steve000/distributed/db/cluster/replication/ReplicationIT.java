package io.steve000.distributed.db.cluster.replication;

import com.sun.net.httpserver.HttpServer;
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

import static org.mockito.Mockito.*;

public class ReplicationIT {

    @Test
    void test() throws ReplicationException, RegistryException, IOException {
        try (
                TestServer server1 = new TestServer();
                TestServer server2 = new TestServer();
                TestServer server3 = new TestServer()
        ) {
            ReplicationHandler replicationHandler = mock(ReplicationHandler.class);

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

            //assert that our handler was called
            verify(replicationHandler, times(1)).handleReplication(replicatable);

            //assert that all other handlers were called
            verify(server1.replicationHandler, times(1)).handleReplication(replicatable);
            verify(server2.replicationHandler, times(1)).handleReplication(replicatable);
            verify(server3.replicationHandler, times(1)).handleReplication(replicatable);
        }
    }

    private class TestServer implements Closeable {

        private final int port;

        private final ReplicationHandler replicationHandler;

        private HttpServer httpServer;

        public TestServer() throws IOException {
            port = NetworkUtils.findOpenPort();
            this.replicationHandler = mock(ReplicationHandler.class);
            httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            httpServer.createContext("/cluster/replication", new ReplicationHttpHandler(replicationHandler));
            httpServer.start();
        }

        @Override
        public void close() {
            httpServer.stop(0);
        }
    }

}
