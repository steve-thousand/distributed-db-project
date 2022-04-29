package io.steve000.distributed.db.registry.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.steve000.distributed.db.registry.api.RegisterRequest;
import io.steve000.distributed.db.registry.api.RegistryResponse;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class RegistryClient {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String registryHost;

    public RegistryClient(final String registryHost) {
        this.registryHost = registryHost;
    }

    public void register(String name, int adminPort) throws IOException {
        RegisterRequest registerRequest = new RegisterRequest(name, adminPort);

        URL url = new URL(registryHost + "/");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);

        objectMapper.writeValue(con.getOutputStream(), registerRequest);

        if(con.getResponseCode() >= 400) {
            throw new RuntimeException("Registry error");
        }
    }

    public RegistryResponse getRegistry() throws IOException {
        URL url = new URL(registryHost + "/");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        if(con.getResponseCode() != 200) {
            throw new RuntimeException("Registry error");
        }

        return objectMapper.readValue(con.getInputStream(), RegistryResponse.class);
    }

}
