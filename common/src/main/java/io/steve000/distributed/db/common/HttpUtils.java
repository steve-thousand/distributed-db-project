package io.steve000.distributed.db.common;

import com.sun.net.httpserver.HttpExchange;

public class HttpUtils {

    public static String getPathFromContext(HttpExchange exchange) {
        return exchange.getRequestURI().getPath().replace(exchange.getHttpContext().getPath(), "");
    }

}
