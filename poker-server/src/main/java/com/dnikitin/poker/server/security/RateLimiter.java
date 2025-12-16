package com.dnikitin.poker.server.security;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter to prevent spam and DOS attacks.
 * Limits number of messages per time window.
 */
@Slf4j
public class RateLimiter {
    private final int maxMessagesPerWindow;
    private final long windowDurationMillis;
    private final Map<String, ClientRateInfo> clientRates;

    private static class ClientRateInfo {
        final AtomicInteger messageCount = new AtomicInteger(0);
        volatile long windowStartTime;

        ClientRateInfo() {
            this.windowStartTime = System.currentTimeMillis();
        }
    }

    /**
     * Creates a rate limiter.
     *
     * @param maxMessagesPerWindow Maximum messages allowed per time window
     * @param windowDurationMillis Duration of the time window in milliseconds
     */
    public RateLimiter(int maxMessagesPerWindow, long windowDurationMillis) {
        this.maxMessagesPerWindow = maxMessagesPerWindow;
        this.windowDurationMillis = windowDurationMillis;
        this.clientRates = new ConcurrentHashMap<>();
    }

    /**
     * Checks if a client is allowed to send a message.
     *
     * @param clientId Unique identifier for the client
     * @return true if allowed, false if rate limit exceeded
     */
    public boolean allowMessage(String clientId) {
        ClientRateInfo rateInfo = clientRates.computeIfAbsent(
            clientId, k -> new ClientRateInfo());

        long now = System.currentTimeMillis();
        long elapsed = now - rateInfo.windowStartTime;

        // Reset window if expired
        if (elapsed > windowDurationMillis) {
            synchronized (rateInfo) {
                if (now - rateInfo.windowStartTime > windowDurationMillis) {
                    rateInfo.windowStartTime = now;
                    rateInfo.messageCount.set(0);
                }
            }
        }

        int count = rateInfo.messageCount.incrementAndGet();

        if (count > maxMessagesPerWindow) {
            log.warn("Rate limit exceeded for client {}: {} msgs in {}ms",
                clientId, count, elapsed);
            return false;
        }

        return true;
    }

    /**
     * Removes a client from rate tracking (e.g., on disconnect).
     *
     * @param clientId Client identifier
     */
    public void removeClient(String clientId) {
        clientRates.remove(clientId);
    }

    /**
     * Cleans up old entries (can be called periodically).
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        clientRates.entrySet().removeIf(entry -> {
            long elapsed = now - entry.getValue().windowStartTime;
            return elapsed > windowDurationMillis * 2;
        });
    }
}
