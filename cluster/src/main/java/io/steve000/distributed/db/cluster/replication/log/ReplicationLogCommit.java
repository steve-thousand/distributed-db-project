package io.steve000.distributed.db.cluster.replication.log;

import io.steve000.distributed.db.cluster.replication.Replicatable;

import java.util.List;
import java.util.Objects;

public class ReplicationLogCommit {

    private final List<Replicatable> replicatables;

    private final int commitNumber;

    public ReplicationLogCommit(List<Replicatable> replicatables, int commitNumber) {
        this.replicatables = replicatables;
        this.commitNumber = commitNumber;
    }

    public List<Replicatable> getReplicatables() {
        return replicatables;
    }

    public int getCommitNumber() {
        return commitNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReplicationLogCommit that = (ReplicationLogCommit) o;
        return commitNumber == that.commitNumber && Objects.equals(replicatables, that.replicatables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(replicatables, commitNumber);
    }

    @Override
    public String toString() {
        return "ReplicationLogCommit{" +
                "replicatables=" + replicatables +
                ", commitNumber=" + commitNumber +
                '}';
    }
}
