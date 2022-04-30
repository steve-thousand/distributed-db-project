package io.steve000.distributed.db.cluster;

public class ReplicationStatus {

    private final String name;

    private final int generation;

    public ReplicationStatus(String name, int generation) {
        this.name = name;
        this.generation = generation;
    }

    public String getName() {
        return name;
    }

    public int getGeneration() {
        return generation;
    }

    public ReplicationStatus increment() {
        return new ReplicationStatus(name, getGeneration() + 1);
    }
}
