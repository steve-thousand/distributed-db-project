package io.steve000.distributed.db.node.server.cluster.election;

import io.steve000.distributed.db.node.server.JSON;
import io.steve000.distributed.db.node.server.cluster.election.api.ElectionRequest;
import io.steve000.distributed.db.node.server.cluster.election.api.VoteResponse;
import io.steve000.distributed.db.registry.api.RegistryEntry;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ElectionClient {

    public VoteResults sendElectionRequests(List<RegistryEntry> registryEntries, LocalDateTime electionStartTime) throws InterruptedException, ExecutionException {
        ElectionRequest electionRequest = new ElectionRequest(electionStartTime);

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        List<Future<VoteResponse>> futures = new ArrayList<>(registryEntries.size());
        for(RegistryEntry entry: registryEntries) {
            Future<VoteResponse> responseFuture = executorService.submit(new VoteRequestThread(entry, electionRequest));
            futures.add(responseFuture);
        }

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        List<VoteResponse> voteResponses = new ArrayList<>(futures.size());
        for(Future<VoteResponse> future: futures) {
            voteResponses.add(future.get());
        }
        return new VoteResults(voteResponses);
    }

    private static class VoteRequestThread implements Callable<VoteResponse> {

        private final RegistryEntry entry;
        private final ElectionRequest electionRequest;

        public VoteRequestThread(RegistryEntry entry, ElectionRequest electionRequest) {
            this.entry = entry;
            this.electionRequest = electionRequest;
        }

        @Override
        public VoteResponse call() throws Exception {
            URL url = new URL("http://" + entry.getHost() + ":" + entry.getPort() + "/cluster/election");
            try {
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");

                byte[] array = JSON.OBJECT_MAPPER.writeValueAsString(electionRequest).getBytes();
                con.setDoOutput(true);
                con.getOutputStream().write(array, 0, array.length);
                con.getOutputStream().flush();
                con.getOutputStream().close();

                if (con.getResponseCode() == 200) {
                    return JSON.OBJECT_MAPPER.readValue(con.getInputStream(), VoteResponse.class);
                }

                con.getInputStream().close();
                throw new ElectionException("Bad response code:" + con.getResponseCode());
            }catch(Exception e) {
                throw new ElectionException("Failed to retrieve election response from " + entry.getName() + " at " + url);
            }
        }
    }

}
