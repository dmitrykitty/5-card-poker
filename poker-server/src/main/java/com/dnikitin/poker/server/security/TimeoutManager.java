package com.dnikitin.poker.server.security;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Manages timeouts for player actions.
 * Automatically folds players who don't act within the timeout period.
 */
@Slf4j
public class TimeoutManager {
    private final long timeoutMillis;
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> activeTimeouts;

    public interface TimeoutCallback {
        void onTimeout(String playerId);
    }

    /**
     * Creates a timeout manager.
     *
     * @param timeoutSeconds Timeout duration in seconds
     */
    public TimeoutManager(int timeoutSeconds) {
        this.timeoutMillis = timeoutSeconds * 1000L;
        this.scheduler = Executors.newScheduledThreadPool(2,
            Thread.ofVirtual().factory());
        this.activeTimeouts = new ConcurrentHashMap<>();
    }

    /**
     * Starts a timeout for a player.
     *
     * @param playerId Player ID
     * @param callback Callback to invoke on timeout
     */
    public void startTimeout(String playerId, TimeoutCallback callback) {
        // Cancel any existing timeout for this player
        cancelTimeout(playerId);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            log.warn("Player {} timed out", playerId);
            activeTimeouts.remove(playerId);
            callback.onTimeout(playerId);
        }, timeoutMillis, TimeUnit.MILLISECONDS);

        activeTimeouts.put(playerId, future);
        log.debug("Timeout started for player {}: {}s", playerId, timeoutMillis / 1000);
    }

    /**
     * Cancels a timeout for a player (called when they act).
     *
     * @param playerId Player ID
     */
    public void cancelTimeout(String playerId) {
        ScheduledFuture<?> future = activeTimeouts.remove(playerId);
        if (future != null) {
            future.cancel(false);
            log.debug("Timeout cancelled for player {}", playerId);
        }
    }

    /**
     * Shuts down the timeout manager.
     */
    public void shutdown() {
        activeTimeouts.values().forEach(f -> f.cancel(false));
        activeTimeouts.clear();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("TimeoutManager shut down");
    }

    /**
     * Checks if a timeout is active for a player.
     *
     * @param playerId Player ID
     * @return true if timeout is active
     */
    public boolean hasActiveTimeout(String playerId) {
        return activeTimeouts.containsKey(playerId);
    }
}
