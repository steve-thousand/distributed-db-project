package io.steve000.distributed.db.cluster.replication.log;

import io.steve000.distributed.db.cluster.replication.Replicatable;
import io.steve000.distributed.db.cluster.replication.ReplicationLogException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ReplicationLogTest {

    @Test
    void log_test() throws IOException, ReplicationLogException {
        File file = File.createTempFile("test", ".log");
        try (ReplicationLog replicationLog = ReplicationLog.open(file)) {
            replicationLog.append(new Replicatable("test", "something"));
            replicationLog.commit();
            replicationLog.append(new Replicatable("hello", "bye"));
            replicationLog.commit();
        }
        String line;
        try (Scanner scanner = new Scanner(file)) {
            line = scanner.next();
            assertEquals("dGVzdA==,c29tZXRoaW5n", line);
            line = scanner.next();
            assertEquals("COMMIT-1", line);
            line = scanner.next();
            assertEquals("aGVsbG8=,Ynll", line);
            line = scanner.next();
            assertEquals("COMMIT-2", line);
            assertFalse(scanner.hasNext());
        }
    }

}
