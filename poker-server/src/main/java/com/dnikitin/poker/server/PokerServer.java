package com.dnikitin.poker.server;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PokerServer {
    private final int port;
    private final ExecutorService executor;
    private volatile boolean running = false;
    private ServerSocketChannel serverSocket;

    public PokerServer(int port) {
        this.port = port;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void start() {
        try {
            serverSocket = ServerSocketChannel.open();
            serverSocket.bind(new InetSocketAddress(port));
            serverSocket.configureBlocking(true);

            running = true;
            log.info("Poker Server started on port: {}", port);

            while (running) {
                try {
                    SocketChannel clientSocket = serverSocket.accept();
                    log.info("New connection accepted: {}", clientSocket.getRemoteAddress());

                    ClientHandler handler = new ClientHandler(clientSocket);
                    executor.submit(handler);
                } catch (IOException e) {
                    if (running) {
                        log.error("Error accepting connection", e);
                    } else {
                        log.info("Server socket closed.");
                    }
                }
            }

        } catch (IOException e) {
            log.error("Could not start server on port {}", port, e);
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && serverSocket.isOpen()) {
                serverSocket.close();
            }

            executor.shutdown();
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            log.info("Server stopped.");
        } catch (IOException | InterruptedException e) {
            log.error("Error while stopping server", e);
        }
    }
}
