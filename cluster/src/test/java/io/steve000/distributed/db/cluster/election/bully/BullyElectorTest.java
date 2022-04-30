package io.steve000.distributed.db.cluster.election.bully;

import com.sun.net.httpserver.HttpServer;
import io.steve000.distributed.db.common.NetworkUtils;
import io.steve000.distributed.db.registry.api.RegistryEntry;
import io.steve000.distributed.db.registry.client.RegistryClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class BullyElectorTest {

    private BullyElector bullyElector;

    @Mock
    private RegistryClient registryClient;

    private AutoCloseable mockitoAutoCloseable;

    @BeforeEach
    void beforeEach() throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", NetworkUtils.findOpenPort()), 0);
        mockitoAutoCloseable = MockitoAnnotations.openMocks(this);
        bullyElector = new BullyElector(registryClient, httpServer);
    }

    @AfterEach
    void afterEach() throws Exception {
        mockitoAutoCloseable.close();
    }

    @Test
    void sendVictoryMessage_test() throws IOException {
        int receivingPort = NetworkUtils.findOpenPort();
        HttpServer receivingServer = HttpServer.create(new InetSocketAddress("0.0.0.0", receivingPort), 0);
        RegistryClient receivingClient = mock(RegistryClient.class);
        BullyElector receivingHandler = new BullyElector(receivingClient, receivingServer);
        receivingServer.setExecutor(Executors.newFixedThreadPool(1));
        receivingServer.start();
        VictoryMessage victoryMessage = new VictoryMessage("your new leader");
        bullyElector.sendVictoryMessage(new RegistryEntry("test", "0.0.0.0", receivingPort), victoryMessage);
        assertEquals(victoryMessage, receivingHandler.victoryMessage);
    }

}
