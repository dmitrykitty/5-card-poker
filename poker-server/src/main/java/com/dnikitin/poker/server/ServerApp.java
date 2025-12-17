package com.dnikitin.poker.server;

public class ServerApp {

    private static final int DEFAULT_PORT = 9999;

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        PokerServer server = new PokerServer(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping server...");
            server.stop();
        }));

        server.start();
    }
}
