package io.steve000.distributed.db.registry.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.steve000.distributed.db.registry.api.RegisterRequest;
import io.steve000.distributed.db.registry.api.RegistryEntry;
import io.steve000.distributed.db.registry.api.RegistryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;

public class RegistryClient {

    private static final Logger logger = LoggerFactory.getLogger(RegistryClient.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String registryHost;

    private final String name;

    private final int nodePort;

    public RegistryClient(String registryHost, String name, int nodePort) {
        this.registryHost = registryHost;
        this.name = name;
        this.nodePort = nodePort;
    }

    public void sendRegistryHeartbeat() throws RegistryException {
        logger.debug("Sending registry heartbeat");
        try {
            RegisterRequest registerRequest = new RegisterRequest(name, nodePort);

            URL url = new URL(registryHost + "/");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);

            objectMapper.writeValue(con.getOutputStream(), registerRequest);

            if (con.getResponseCode() >= 400) {
                throw new RuntimeException("Registry error");
            }
        }catch(Exception e) {
            throw new RegistryException(e);
        }
    }

    public RegistryResponse getRegistry() throws RegistryException {
        try {
            URL url = new URL(registryHost + "/");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            if (con.getResponseCode() != 200) {
                throw new RuntimeException("Registry error");
            }

            return objectMapper.readValue(con.getInputStream(), RegistryResponse.class);
        }catch(Exception e) {
            throw new RegistryException(e);
        }
    }

    public RegistryEntry getRegistryEntryByName(String name) throws RegistryException {
        return getRegistry().getRegistryEntries().stream()
                .filter(r -> r.getName().equals(name))
                .findFirst().orElseThrow(() -> new RegistryException("No registry entry found by name " + name));
    }

}
