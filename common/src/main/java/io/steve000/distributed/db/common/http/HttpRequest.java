package io.steve000.distributed.db.common.http;

import io.steve000.distributed.db.common.JSON;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpRequest {

    public static class Builder {

        private final String url;

        public Builder(final String url) {
            this.url = url;
        }

        public HttpResponse get() throws IOException {
            URL url = new URL(this.url);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            return new HttpResponse(con);
        }

        public HttpResponse post() throws IOException {
            return post(null);
        }

        public HttpResponse post(Object body) throws IOException {
            URL url = new URL(this.url);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            if (body != null) {
                con.setDoOutput(true);
                JSON.OBJECT_MAPPER.writeValue(con.getOutputStream(), body);
            }
            return new HttpResponse(con);
        }

    }

}
