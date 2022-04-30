package io.steve000.distributed.db.node.server;

import picocli.CommandLine;

public class DBArgs {

    @CommandLine.Option(names = "--admin-port", defaultValue = "8080",
            description = "Port for administrative actions.")
    int adminPort;


    @CommandLine.Option(names = {"-ra", "--registry-address"}, required = true,
            description = "Address of registry service.")
    String registryAddress;

}
