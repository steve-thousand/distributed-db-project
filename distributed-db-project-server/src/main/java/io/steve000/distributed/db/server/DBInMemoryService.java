package io.steve000.distributed.db.server;

import java.util.HashMap;
import java.util.Map;

public class DBInMemoryService implements DBService {

    private final Map<String, String> inMemoryDb = new HashMap<String, String>();

    public void set(String key, String value) {
        inMemoryDb.put(key, value);
    }

    public String get(String key) {
        return inMemoryDb.get(key);
    }
}
