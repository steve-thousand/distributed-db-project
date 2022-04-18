package io.steve000.distributed.db.node.server;

public interface DBService {
    void set(String key, String value);

    String get(String key);
}
