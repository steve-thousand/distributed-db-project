package io.steve000.distributed.db.node.server.cluster;

public class Leader {

    private final String name;

    private final boolean self;

    public Leader(String name, boolean self) {
        this.name = name;
        this.self = self;
    }

    public String getName() {
        return name;
    }

    public boolean isSelf() {
        return self;
    }
}
