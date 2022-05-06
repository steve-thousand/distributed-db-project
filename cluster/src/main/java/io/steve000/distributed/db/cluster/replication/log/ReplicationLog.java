package io.steve000.distributed.db.cluster.replication.log;

import io.steve000.distributed.db.cluster.replication.Replicatable;
import io.steve000.distributed.db.cluster.replication.ReplicationLogException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReplicationLog implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(ReplicationLog.class);

    private static final Base64.Encoder encoder = Base64.getEncoder();

    private final File file;

    private final FileWriter fileWriter;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final BlockingQueue<Replicatable> blockingQueue;

    //TODO threadsafe
    private int commitNumber = 0;

    private Replicatable lastReplicatable;

    private ReplicationLog(File file, FileWriter fileWriter) {
        this.file = file;
        this.fileWriter = fileWriter;
        blockingQueue = new LinkedBlockingDeque<>(10);
    }

    public void sync(InputStream inputStream) throws ReplicationLogException {
        try (Scanner scanner = new Scanner(inputStream)) {
            Base64.Decoder decoder = Base64.getDecoder();
            while (scanner.hasNext()) {
                String line = scanner.next();
                String[] parts = line.split(",");
                Replicatable replicatable = new Replicatable(
                        new String(decoder.decode(parts[0])),
                        new String(decoder.decode(parts[1]))
                );
                append(replicatable);
                commit();
            }
        }
    }

    public void sync(OutputStream outputStream) throws IOException {
        try (Scanner scanner = new Scanner(file);
             OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
            String line;
            while (scanner.hasNext()) {
                line = scanner.next();
                if (line.startsWith("COMMIT-")) {
                    continue;
                }
                writer.write(line);
                writer.write('\n');
            }
            writer.flush();
        }
    }

    public void append(Replicatable replicatable) throws ReplicationLogException {
        readWriteLock.writeLock().lock();
        try {
            fileWriter.write(buildLogLine(replicatable));
            fileWriter.flush();
            this.lastReplicatable = replicatable;
        } catch (IOException e) {
            throw new ReplicationLogException("Failed to append to log", e);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public void commit() throws ReplicationLogException {
        readWriteLock.writeLock().lock();
        try {
            blockingQueue.put(lastReplicatable);
            commitNumber++;
            lastReplicatable = null;
            logger.info("Replication log commit {}", commitNumber);
        } catch (InterruptedException e) {
            throw new ReplicationLogException("Failed to commit index" + commitNumber, e);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private static String buildLogLine(Replicatable replicatable) {
        return encoder.encodeToString(replicatable.getKey().getBytes(StandardCharsets.UTF_8)) +
                "," +
                encoder.encodeToString(replicatable.getValue().getBytes(StandardCharsets.UTF_8)) +
                "\n";
    }

    public Replicatable blockingRead() throws InterruptedException {
        return blockingQueue.take();
    }

    @Override
    public void close() throws IOException {
        fileWriter.close();
    }

    public static ReplicationLog open() {
        try {
            File file = File.createTempFile("replication", ".log");
            return open(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ReplicationLog open(File file) throws IOException {
        FileWriter fileWriter = new FileWriter(file);
        return new ReplicationLog(file, fileWriter);
    }
}
