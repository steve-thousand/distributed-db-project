package io.steve000.distributed.db.cluster;

import com.sun.net.httpserver.HttpServer;

import java.io.Closeable;

public interface ClusterService extends Closeable {

    void bind(HttpServer httpServer);

    Leader getLeader();

    void register(String name, int adminPort);

    void handleHeartBeat(HeartBeat heartBeat);

    void sendHeartBeats();

}
