package io.steve000.distributed.db.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URI;

public class DBHandler implements HttpHandler {

    private final DBInMemoryService dbService = new DBInMemoryService();

    public void handle(HttpExchange httpExchange) throws IOException {
        if ("GET".equals(httpExchange.getRequestMethod())) {
            final String key = getKeyFromUri(httpExchange.getRequestURI());
            final String value = dbService.get(key);
            if (value == null) {
                httpExchange.sendResponseHeaders(404, 0);
            } else {
                byte[] charArray = value.getBytes();
                httpExchange.sendResponseHeaders(200, charArray.length);
                httpExchange.getResponseBody().write(charArray, 0, charArray.length);
            }
        } else if ("POST".equals(httpExchange.getRequestMethod())) {
            final String key = getKeyFromUri(httpExchange.getRequestURI());
            final String value = getValueFromBody(httpExchange);
            dbService.set(key, value);
            httpExchange.sendResponseHeaders(201, 0);
        }
        httpExchange.getResponseBody().flush();
        httpExchange.getResponseBody().close();
    }

    static String getKeyFromUri(URI uri) {
        return uri.getPath().substring(1);
    }

    static String getValueFromBody(HttpExchange httpExchange) throws IOException {
        int length = Integer.parseInt(httpExchange.getRequestHeaders().get("Content-Length").get(0));
        byte[] postArray = new byte[length];
        httpExchange.getRequestBody().read(postArray, 0, length);
        return new String(postArray);
    }
}
