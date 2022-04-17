package io.steve000.distributed.db.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DistributedDBClient {

    private final String host;

    public DistributedDBClient(String host) {
        this.host = host;
    }

    public void set(String key, String value) throws IOException {
        URL url = new URL(host + "/" + key);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");

        byte[] array = value.getBytes();
        con.setDoOutput(true);
        con.getOutputStream().write(array, 0, array.length);
        con.getOutputStream().flush();
        con.getOutputStream().close();
    }

    public String get(String key) throws IOException {
        URL url = new URL(host + "/" + key);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        int statusCode = con.getResponseCode();
        if (statusCode == 404) {
            return null;
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        return content.toString();
    }

}
