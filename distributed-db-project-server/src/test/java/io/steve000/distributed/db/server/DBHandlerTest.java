package io.steve000.distributed.db.server;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DBHandlerTest {

    @Test
    void getKeyFromUri_test() throws URISyntaxException {
        URI uri = new URI("http://localhost/testKey");
        final String foundKey = DBHandler.getKeyFromUri(uri);
        assertEquals("testKey", foundKey);
    }

}
