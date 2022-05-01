package io.steve000.distributed.db.cluster;

public class ClusterConfig {

    private String name;

    private long custerThreadPeriodMs = 100;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCusterThreadPeriodMs() {
        return custerThreadPeriodMs;
    }

    public void setCusterThreadPeriodMs(long custerThreadPeriodMs) {
        this.custerThreadPeriodMs = custerThreadPeriodMs;
    }
}
