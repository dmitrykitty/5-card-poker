package com.dnikitin.poker.server;

import com.dnikitin.poker.server.security.RateLimiter;
import com.dnikitin.poker.server.security.TimeoutManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main poker server with security features.
 * Uses java.nio for network I/O and virtual threads for scalability.
 */
@Slf4j
public class PokerServer {
    private static final int MAX_MESSAGES_PER_SECOND = 10;
    private static final int RATE_LIMIT_WINDOW_MS = 1000;
    private static final int TURN_TIMEOUT_SECONDS = 45;
    private static final int CLEANUP_INTERVAL_MINUTES = 5;

    /**
     * -- GETTER --
     *  Gets the port the server is running on.
     */
    @Getter
    private final int port;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduledExecutor;
    private final RateLimiter rateLimiter;
    private final TimeoutManager timeoutManager;

    /**
     * -- GETTER --
     *  Checks if the server is running.
     */
    @Getter
    private volatile boolean running = false;
    private ServerSocketChannel serverSocket;

    public PokerServer(int port) {
        this.port = port;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.scheduledExecutor = Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory());
        this.rateLimiter = new RateLimiter(MAX_MESSAGES_PER_SECOND, RATE_LIMIT_WINDOW_MS);
        this.timeoutManager = new TimeoutManager(TURN_TIMEOUT_SECONDS);

        log.info("PokerServer initialized with security features:");
        log.info("  - Rate limit: {} messages per second", MAX_MESSAGES_PER_SECOND);
        log.info("  - Turn timeout: {} seconds", TURN_TIMEOUT_SECONDS);
    }

    public void start() {
        try {
            serverSocket = ServerSocketChannel.open();
            serverSocket.bind(new InetSocketAddress(port));
            serverSocket.configureBlocking(true);

            running = true;
            log.info("Poker Server started on port: {}", port);
            log.info("Using virtual threads for client handling");

            // Schedule periodic cleanup of rate limiter
            scheduledExecutor.scheduleAtFixedRate(
                rateLimiter::cleanup,
                CLEANUP_INTERVAL_MINUTES,
                CLEANUP_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            );

            while (running) {
                try {
                    SocketChannel clientSocket = serverSocket.accept();
                    log.info("New connection accepted: {}", clientSocket.getRemoteAddress());

                    // Create handler with security components
                    ClientHandler handler = new ClientHandler(
                        clientSocket,
                        rateLimiter,
                        timeoutManager
                    );
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
        log.info("Shutting down server...");

        try {
            if (serverSocket != null && serverSocket.isOpen()) {
                serverSocket.close();
            }

            // Shutdown timeout manager
            timeoutManager.shutdown();

            // Shutdown scheduled executor
            scheduledExecutor.shutdown();
            if (!scheduledExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }

            // Shutdown main executor
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Forcing shutdown of client handlers");
                executor.shutdownNow();
            }

            log.info("Server stopped successfully.");
        } catch (IOException | InterruptedException e) {
            log.error("Error while stopping server", e);
            Thread.currentThread().interrupt();
        }
    }

}
