package io.steve000.distributed.db.cluster;

import com.sun.net.httpserver.HttpServer;

/**
 * I LIKE INTERFACES EVEN IF THEY ONLY HAVE ONE IMPLEMENTATION BECAUSE THEY ENFORCE A LAYER OF SEPARATION, OK?
 */
public interface ClusterService {

    void bind(HttpServer httpServer);

    Leader getLeader();

    void register(String name, int adminPort);

    void unregister();

    void handleHeartBeat(HeartBeat heartBeat);

    void sendHeartBeats();

}
