package io.steve000.distributed.db.cluster;

import java.util.Objects;

public class Leader {

    private final String name;

    private final boolean self;

    private final String host;

    private final int port;

    public Leader(String name, boolean self, String host, int port) {
        this.name = name;
        this.self = self;
        this.host = host;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public boolean isSelf() {
        return self;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Leader leader = (Leader) o;
        return self == leader.self && port == leader.port && Objects.equals(name, leader.name) && Objects.equals(host, leader.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, self, host, port);
    }

    @Override
    public String toString() {
        return "Leader{" +
                "name='" + name + '\'' +
                ", self=" + self +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
