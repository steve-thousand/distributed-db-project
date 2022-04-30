package io.steve000.distributed.db.cluster;

import java.util.Objects;

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

    @Override
    public String toString() {
        return "Leader{" +
                "name='" + name + '\'' +
                ", self=" + self +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Leader leader = (Leader) o;
        return self == leader.self && Objects.equals(name, leader.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, self);
    }
}
