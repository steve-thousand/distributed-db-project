package io.steve000.distributed.db.cluster;

public class ReplicationStatus {

    private final String name;

    private int generation;

    public ReplicationStatus(String name) {
        this(name, 0);
    }

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

    public void increment() {
        //TODO atomic
        generation++;
    }
}
