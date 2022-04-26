package io.steve000.distributed.db.cluster;

import com.sun.net.httpserver.HttpServer;
import io.steve000.distributed.db.common.JSON;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class ClusterHttpHandlerIT {

    @Mock
    private ClusterService clusterService;

    private AutoCloseable mockitoAnnotationCloseable;

    private InetSocketAddress address;

    private HttpServer httpServer;

    @BeforeEach
    void beforeEach() throws IOException {
        mockitoAnnotationCloseable = MockitoAnnotations.openMocks(this);
        ClusterHttpHandler httpHandler = new ClusterHttpHandler(clusterService);
        address = new InetSocketAddress(findOpenPort());
        httpServer = HttpServer.create(address, 0);
        httpServer.createContext("/test", httpHandler);
        httpServer.start();
    }

    @AfterEach
    void afterEach() throws Exception {
        httpServer.stop(0);
        verifyNoMoreInteractions(clusterService);
        mockitoAnnotationCloseable.close();
    }

    @Test
    void testHeartBeat() throws IOException {
        URL url = new URL("http://localhost:" + address.getPort() + "/test/heartbeat");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);

        HeartBeat heartBeat = new HeartBeat("test");
        JSON.OBJECT_MAPPER.writeValue(con.getOutputStream(), heartBeat);

        assertEquals(201, con.getResponseCode());
        verify(clusterService, times(1)).handleHeartBeat(heartBeat);
    }

    private int findOpenPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

}
