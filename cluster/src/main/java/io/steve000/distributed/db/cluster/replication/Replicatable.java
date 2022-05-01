package io.steve000.distributed.db.cluster.replication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Needed to have some clean way of marshalling JSON, so this is the forced API. If an implementation
 * wants, then some custom JSON marshalling could be done within the value, with keys used as typing.
 * Not great, but at this point I don't really care.
 */
public class Replicatable {

    private final String key;

    private final String value;

    @JsonCreator
    public Replicatable(@JsonProperty("key") String key, @JsonProperty("value") String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Replicatable that = (Replicatable) o;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return "Replicatable{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
