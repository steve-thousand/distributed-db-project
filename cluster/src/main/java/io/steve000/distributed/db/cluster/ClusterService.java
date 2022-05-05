package io.steve000.distributed.db.cluster;

import com.sun.net.httpserver.HttpServer;
import io.steve000.distributed.db.cluster.replication.ReplicationService;

public interface ClusterService extends AutoCloseable {

    void bind(HttpServer httpServer);

    Leader getLeader();

    void run();

    void handleHeartBeat(HeartBeat heartBeat);

    ReplicationService replicationService();

}
