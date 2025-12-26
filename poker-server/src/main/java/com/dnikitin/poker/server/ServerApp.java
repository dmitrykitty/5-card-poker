package com.dnikitin.poker.server;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Entry point for the Poker Server application.
 * <p>
 * Parses command-line arguments to determine the port and starts the main {@link PokerServer} instance.
 * It also registers a shutdown hook to ensure graceful termination of server resources when the JVM stops.
 * </p>
 */
public class ServerApp {

    private static final int DEFAULT_PORT = 9999;

    /**
     * The main method.
     *
     * @param args Command line arguments. The first argument is optional and specifies the port number.
     */
    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));


        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        PokerServer server = new PokerServer(port);

        // Ensure resources (sockets, threads) are closed properly on Ctrl+C or kill
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping server...");
            server.stop();
        }));

        server.start();
    }
}