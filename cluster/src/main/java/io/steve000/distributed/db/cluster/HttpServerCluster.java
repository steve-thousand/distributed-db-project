package io.steve000.distributed.db.cluster;

import com.sun.net.httpserver.HttpServer;

public class HttpServerCluster {

    public HttpServerCluster(HttpServer httpServer, ClusterService clusterService) {
        httpServer.createContext("/cluster", new ClusterHttpHandler(clusterService));
    }

}
