package io.steve000.distributed.db.cluster.election.bully;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.steve000.distributed.db.cluster.Leader;
import io.steve000.distributed.db.cluster.ReplicationStatus;
import io.steve000.distributed.db.cluster.election.ElectionException;
import io.steve000.distributed.db.cluster.election.Elector;
import io.steve000.distributed.db.common.JSON;
import io.steve000.distributed.db.registry.api.RegistryEntry;
import io.steve000.distributed.db.registry.client.RegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public class BullyElector implements Elector, HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(BullyElector.class);

    private final RegistryClient registryClient;

    VictoryMessage victoryMessage;

    public BullyElector(RegistryClient registryClient, HttpServer httpServer) {
        this.registryClient = registryClient;
        httpServer.createContext("/cluster/election", this);
    }

    @Override
    public Leader electLeader(ReplicationStatus replicationStatus) throws ElectionException {
        try {
            List<RegistryEntry> registryEntries = registryClient.getRegistry().getRegistryEntries();

            //find all higher-named registered nodes and send election requests
            List<RegistryEntry> higherEntries = registryEntries.stream()
                    .filter(e -> e.getName().compareTo(replicationStatus.getName()) > 0)
                    .collect(Collectors.toList());

            if(higherEntries.size() > 0) {
                Leader foundHigherLeader = findHigherLeader(higherEntries);
                if (foundHigherLeader != null) {
                    return foundHigherLeader;
                }
                //if we get no responses, all higher nodes are down, and we are the leader! tell everyone
                logger.info("Found no higher leader, we ({}) are the leader now!", replicationStatus.getName());
            }else{
                logger.info("No higher nodes, we ({}) are the leader now!", replicationStatus.getName());
            }

            VictoryMessage victoryMessage = new VictoryMessage(replicationStatus.getName());
            sendVictoryMessages(registryEntries.stream()
                    .filter(e -> e.getName().compareTo(replicationStatus.getName()) < 0)
                    .collect(Collectors.toList()), victoryMessage);
            return new Leader(replicationStatus.getName(), true);
        } catch (Exception e) {
            throw new ElectionException(e);
        }
    }

    private Leader findHigherLeader(List<RegistryEntry> higherEntries) throws ElectionException {
        try {
            ExecutorService victoryMessageWaitExecutor = Executors.newSingleThreadExecutor();
            Future<VictoryMessage> victoryMessageFuture = victoryMessageWaitExecutor.submit(() -> {
                //wait for a victory message from anyone
                while (true) {
                    if (victoryMessage != null) {
                        return victoryMessage;
                    }
                    sleep(100);
                }
            });
            victoryMessageWaitExecutor.shutdown();

            logger.info("Sending election requests to {} higher nodes", higherEntries.size());

            ExecutorService higherNodeElectionExecutor = Executors.newSingleThreadExecutor();
            List<Future<Boolean>> aliveResponses = higherEntries.stream().map(entry -> higherNodeElectionExecutor.submit(() -> {
                URL url = new URL("http://" + entry.getHost() + ":" + entry.getPort() + "/cluster/election");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.getOutputStream().close();
                return con.getResponseCode() == HttpURLConnection.HTTP_OK;
            })).collect(Collectors.toList());
            higherNodeElectionExecutor.shutdown();
            higherNodeElectionExecutor.awaitTermination(2, TimeUnit.SECONDS);

            logger.info("Processing election responses...");
            boolean oneAlive = false;
            for (Future<Boolean> aliveResponse : aliveResponses) {
                try {
                    if (Boolean.TRUE.equals(aliveResponse.get(2, TimeUnit.SECONDS))) {
                        logger.debug("One alive!");
                        oneAlive = true;
                        break;
                    }else{
                        logger.debug("One not alive.");
                    }
                }catch(Exception e) {
                    logger.error("Error when calling higher node", e);
                }
            }

            if (oneAlive) {
                logger.info("Received at least one live response, waiting for a victory message from on high.");
                victoryMessageWaitExecutor.awaitTermination(5, TimeUnit.SECONDS);
                VictoryMessage victoryMessage = victoryMessageFuture.get();
                if (victoryMessage != null) {
                    logger.debug("Received a victory message! {}", victoryMessage);
                    return new Leader(victoryMessage.getName(), false);
                }
            } else {
                logger.info("Found no live responses in time.");
            }

            return null;
        } catch (Exception e) {
            throw new ElectionException(e);
        }
    }

    void sendVictoryMessages(List<RegistryEntry> entries, VictoryMessage victoryMessage) throws InterruptedException {
        ExecutorService victoryMessageWaitExecutor = Executors.newSingleThreadExecutor();
        for(RegistryEntry entry: entries) {
            victoryMessageWaitExecutor.submit(() -> {
                try {
                    sendVictoryMessage(entry, victoryMessage);
                }catch(Exception e) {
                    logger.error("Failed to send victory to entry {}", entry);
                }
            });
        }
        victoryMessageWaitExecutor.shutdown();
        victoryMessageWaitExecutor.awaitTermination(5, TimeUnit.SECONDS);
    }

    void sendVictoryMessage(RegistryEntry entry, VictoryMessage victoryMessage) throws IOException {
        URL url = new URL("http://" + entry.getHost() + ":" + entry.getPort() + "/cluster/election/victory");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setConnectTimeout(5000);
        con.setDoOutput(true);
        JSON.OBJECT_MAPPER.writeValue(con.getOutputStream(), victoryMessage);
        con.getOutputStream().close();
        con.getResponseCode();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            if(exchange.getRequestURI().getPath().replace(exchange.getHttpContext().getPath(), "").equals("/victory")){
                victoryMessage = JSON.OBJECT_MAPPER.readValue(exchange.getRequestBody(), VictoryMessage.class);
                logger.info("Received victory message! {}", victoryMessage);
                Leader leader = new Leader(victoryMessage.getName(), false);
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().close();
            }
        }
    }
}
