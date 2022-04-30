package io.steve000.distributed.db.common;

import java.io.IOException;
import java.net.ServerSocket;

public class NetworkUtils {
    public static int findOpenPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
