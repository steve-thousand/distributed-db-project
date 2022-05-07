package io.steve000.distributed.db.common.http;

import io.steve000.distributed.db.common.JSON;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class HttpResponse implements AutoCloseable {

    private final HttpURLConnection connection;

    private int responseCode = -1;

    HttpResponse(final HttpURLConnection connection) {
        this.connection = connection;
    }

    public int getResponseCode() throws IOException {
        if(responseCode == -1) {
            responseCode = connection.getResponseCode();
        }
        return responseCode;
    }

    public <T> T read(Class<T> clazz) throws IOException {
        return JSON.OBJECT_MAPPER.readValue(connection.getInputStream(), clazz);
    }

    public InputStream read() throws IOException {
        return connection.getInputStream();
    }

    @Override
    public void close() throws Exception {
        connection.getInputStream().close();
    }
}
