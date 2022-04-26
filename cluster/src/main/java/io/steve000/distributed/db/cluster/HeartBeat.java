package io.steve000.distributed.db.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class HeartBeat {

    private final String name;

    @JsonCreator
    public HeartBeat(@JsonProperty("name") String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "HeartBeat{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeartBeat heartBeat = (HeartBeat) o;
        return Objects.equals(name, heartBeat.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
