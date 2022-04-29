package io.steve000.distributed.db.cluster;

public class ClusterConfig {

    private long custerThreadPeriodMs = 3000;

    public long getCusterThreadPeriodMs() {
        return custerThreadPeriodMs;
    }

    public void setCusterThreadPeriodMs(long custerThreadPeriodMs) {
        this.custerThreadPeriodMs = custerThreadPeriodMs;
    }
}
