package io.steve000.distributed.db.server;

import picocli.CommandLine;

public class DBArgs {

    @CommandLine.Option(names = "--admin-port", defaultValue = "8050",
            description = "Port for administrative actions.")
    int adminPort;

}
