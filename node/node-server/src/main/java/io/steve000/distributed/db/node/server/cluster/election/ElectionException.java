package io.steve000.distributed.db.node.server.cluster.election;

public class ElectionException extends Exception {

    public ElectionException(String message) {
        super(message);
    }

    public ElectionException(Throwable e) {
        super(e);
    }
}
