package io.steve000.distributed.db.server;

public interface DBService {
    void set(String key, String value);

    String get(String key);
}
