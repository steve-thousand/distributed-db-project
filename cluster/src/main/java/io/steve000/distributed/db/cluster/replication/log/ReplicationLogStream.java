package io.steve000.distributed.db.cluster.replication.log;

import io.steve000.distributed.db.cluster.replication.Replicatable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;

public class ReplicationLogStream implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ReplicationLogStream.class);

    private final int startingCommit;

    private final Scanner scanner;

    private final Lock readLock;

    private boolean preScanned = false;

    public ReplicationLogStream(int startingCommit, Scanner scanner, Lock readLock) {
        this.startingCommit = startingCommit;
        this.scanner = scanner;
        this.readLock = readLock;
    }

    public ReplicationLogCommit readCommit() {
        List<Replicatable> replicatables = new ArrayList<>();

        //if startingCommit == 0 then we are reading from the beginning
        if (!preScanned && startingCommit > 0) {
            //scan to starting commit
            while (scanner.hasNext()) {
                final String line = scanner.next();
                if (line.equals("COMMIT-" + startingCommit)) {
                    preScanned = true;
                    break;
                }
            }
        }

        while (scanner.hasNext()) {
            final String line = scanner.next();

            if (line.startsWith("COMMIT-")) {
                int commitNumber = Integer.parseInt(line.replace("COMMIT-", ""));
                return new ReplicationLogCommit(replicatables, commitNumber);
            }
            String[] parts = line.split(",");
            replicatables.add(new Replicatable(
                    new String(Base64.getDecoder().decode(parts[0])),
                    new String(Base64.getDecoder().decode(parts[1]))
                    ));
        }
        if (replicatables.size() > 0) {
            logger.error("Unexpected end of replication log stream, replicatables without commit number: {}", replicatables);
        }
        return null;
    }

    public void write(OutputStream outputStream) {

    }

    @Override
    public void close() throws Exception {
        scanner.close();
        //lol this is kind of weird but im mad with async power.
        //TODO in fact it might not be necessary...
        readLock.unlock();
    }
}
