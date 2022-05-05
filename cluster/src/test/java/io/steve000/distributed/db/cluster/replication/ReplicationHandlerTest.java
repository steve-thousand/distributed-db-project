package io.steve000.distributed.db.cluster.replication;

import io.steve000.distributed.db.cluster.replication.log.ReplicationLog;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class ReplicationHandlerTest {

    @Test
    void test() throws ReplicationLogException {
        ReplicationLog replicationLog = mock(ReplicationLog.class);
        ReplicationHandler replicationHandler = new ReplicationHandler(replicationLog, mock(ReplicationReceiver.class));
        Replicatable replicatable = new Replicatable("test", "something");
        replicationHandler.appendToLog(replicatable);
        verify(replicationLog, times(1)).append(replicatable);
    }

}
