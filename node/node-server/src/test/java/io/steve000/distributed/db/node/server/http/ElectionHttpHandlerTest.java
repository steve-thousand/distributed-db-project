package io.steve000.distributed.db.node.server.http;

import com.sun.net.httpserver.HttpExchange;
import io.steve000.distributed.db.node.server.cluster.ReplicationStatus;
import io.steve000.distributed.db.node.server.cluster.election.ElectionRequest;
import io.steve000.distributed.db.node.server.cluster.election.ElectionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.*;
import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElectionHttpHandlerTest {

    @Mock
    private ElectionService electionService;

    @Mock
    private ReplicationStatus replicationStatus;

    private AutoCloseable mockitoAnnotationCloseable;

    @BeforeEach
    void beforeEach() {
        mockitoAnnotationCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void afterEach() throws Exception {
        mockitoAnnotationCloseable.close();
    }

    @Test
    void handle_test() throws IOException {
        ElectionHttpHandler httpHandler = new ElectionHttpHandler(electionService, replicationStatus);
        HttpExchange httpExchange = mock(HttpExchange.class);
        when(httpExchange.getRequestMethod()).thenReturn("POST");

        ElectionRequest request = new ElectionRequest(LocalDateTime.now());
        String json = ElectionHttpHandler.objectMapper.writeValueAsString(request);
        when(httpExchange.getRequestBody()).thenReturn(new ByteArrayInputStream(json.getBytes()));

        OutputStream outputStream = new ByteArrayOutputStream();
        when(httpExchange.getResponseBody()).thenReturn(outputStream);

        httpHandler.handle(httpExchange);
    }

}
