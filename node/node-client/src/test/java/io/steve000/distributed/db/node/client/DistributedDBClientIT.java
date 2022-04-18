package io.steve000.distributed.db.node.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Need to manually run server to point this at for now.
 */
public class DistributedDBClientIT {

    private DistributedDBClient distributedDBClient;

    @BeforeEach
    void beforeEach() {
        distributedDBClient = new DistributedDBClient("http://localhost:8080");
    }

    @Test
    void test() throws IOException {
        distributedDBClient.set("testKey1", "testValue1");
        String foundValue1 = distributedDBClient.get("testKey1");
        assertEquals("testValue1", foundValue1);

        String foundValue2 = distributedDBClient.get("testKey2");
        assertNull(foundValue2);

        distributedDBClient.set("testKey2", "testValue2");
        String foundValue3 = distributedDBClient.get("testKey2");
        assertEquals("testValue2", foundValue3);
    }

}
