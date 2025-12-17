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
 * The core TCP server implementation for the Poker game.
 * <p>
 * <b>Concurrency Model:</b>
 * This server utilizes Java 21+ <b>Virtual Threads</b> (via {@link Executors#newVirtualThreadPerTaskExecutor()}).
 * Unlike traditional thread pools, virtual threads are lightweight entities managed by the JVM, allowing
 * the server to handle thousands of concurrent connections with a simple "thread-per-client" model.
 * Even though the socket is configured in blocking mode, blocking operations (like I/O) unmount the
 * virtual thread from the OS carrier thread, ensuring high throughput.
 * </p>
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 * <li>Accepting incoming TCP connections.</li>
 * <li>Managing the lifecycle of connection handlers.</li>
 * <li>Initializing security components (Rate Limiting, Timeouts).</li>
 * </ul>
 * </p>
 */
@Slf4j
public class PokerServer {
    private static final int MAX_MESSAGES_PER_SECOND = 10;
    private static final int RATE_LIMIT_WINDOW_MS = 1000;
    private static final int TURN_TIMEOUT_SECONDS = 45;
    private static final int CLEANUP_INTERVAL_MINUTES = 5;

    /**
     * The port number this server is bound to.
     */
    @Getter
    private final int port;

    /**
     * Executor service responsible for spawning a new virtual thread for each connected client.
     */
    private final ExecutorService executor;

    /**
     * Scheduler for background maintenance tasks (e.g., clearing rate limiter caches).
     */
    private final ScheduledExecutorService scheduledExecutor;

    private final RateLimiter rateLimiter;
    private final TimeoutManager timeoutManager;

    /**
     * Indicates whether the server's main accept loop is currently active.
     */
    @Getter
    private volatile boolean running = false;
    private ServerSocketChannel serverSocket;

    /**
     * Initializes the server configuration and security subsystems.
     *
     * @param port The port to bind to (e.g., 9999).
     */
    public PokerServer(int port) {
        this.port = port;
        // Use virtual threads to handle high concurrency with blocking I/O simplicity
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.scheduledExecutor = Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory());
        this.rateLimiter = new RateLimiter(MAX_MESSAGES_PER_SECOND, RATE_LIMIT_WINDOW_MS);
        this.timeoutManager = new TimeoutManager(TURN_TIMEOUT_SECONDS);

        log.info("PokerServer initialized with security features:");
        log.info("  - Rate limit: {} messages per second", MAX_MESSAGES_PER_SECOND);
        log.info("  - Turn timeout: {} seconds", TURN_TIMEOUT_SECONDS);
    }

    /**
     * Starts the server loop.
     * <p>
     * This method binds the server socket and enters a blocking loop that accepts incoming connections.
     * For each connection, a new {@link ClientHandler} is instantiated and submitted to the virtual thread executor.
     * </p>
     * <p>
     * <b>Note:</b> This method blocks the calling thread until {@link #stop()} is called or a critical error occurs.
     * </p>
     */
    public void start() {
        try {
            serverSocket = ServerSocketChannel.open();
            serverSocket.bind(new InetSocketAddress(port));
            // Blocking mode is used intentionally because Virtual Threads handle blocking efficiently
            serverSocket.configureBlocking(true);

            running = true;
            log.info("Poker Server started on port: {}", port);
            log.info("Using virtual threads for client handling");

            // Schedule periodic cleanup of rate limiter to prevent memory leaks
            scheduledExecutor.scheduleAtFixedRate(
                    rateLimiter::cleanup,
                    CLEANUP_INTERVAL_MINUTES,
                    CLEANUP_INTERVAL_MINUTES,
                    TimeUnit.MINUTES
            );

            while (running) {
                try {
                    // Blocks until a new client connects
                    SocketChannel clientSocket = serverSocket.accept();
                    log.info("New connection accepted: {}", clientSocket.getRemoteAddress());

                    // Create handler with injected security components
                    ClientHandler handler = new ClientHandler(
                            clientSocket,
                            rateLimiter,
                            timeoutManager
                    );
                    // Pass processing to a virtual thread
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

    /**
     * Stops the server gracefully.
     * <p>
     * Closes the server socket to stop accepting new connections, shuts down the executors,
     * and cleans up resources. Active client handlers might be terminated abruptly if they
     * do not exit within the timeout period.
     * </p>
     */
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