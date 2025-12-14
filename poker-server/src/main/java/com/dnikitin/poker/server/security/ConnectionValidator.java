package com.dnikitin.poker.server.security;

import com.dnikitin.poker.common.exceptions.ProtocolException;
import com.dnikitin.poker.common.exceptions.PokerSecurityException;
import lombok.extern.slf4j.Slf4j;

/**
 * Validates incoming connections and messages for security.
 */
@Slf4j
public class ConnectionValidator {
    private static final int MAX_MESSAGE_SIZE = 512;
    private static final int MAX_COMMAND_PARTS = 20;

    /**
     * Validates a message before processing.
     *
     * @param message The message to validate
     * @throws PokerSecurityException if message fails security checks
     * @throws ProtocolException if message is malformed
     */
    public void validateMessage(String message) {
        if (message == null) {
            throw new ProtocolException("NULL_MESSAGE", "Message is null");
        }

        // Check message size
        if (message.length() > MAX_MESSAGE_SIZE) {
            log.warn("Message too large: {} bytes", message.length());
            throw new PokerSecurityException("MESSAGE_TOO_LARGE",
                String.format("Message exceeds %d bytes", MAX_MESSAGE_SIZE));
        }

        // Check for suspicious patterns
        if (message.contains("\0") || message.contains("\r\n")) {
            log.warn("Suspicious characters in message");
            throw new PokerSecurityException("INVALID_CHARACTERS",
                "Message contains invalid characters");
        }

        // Check number of parts (prevent DOS via excessive parsing)
        String[] parts = message.split("\\s+");
        if (parts.length > MAX_COMMAND_PARTS) {
            log.warn("Too many command parts: {}", parts.length);
            throw new PokerSecurityException("TOO_MANY_PARTS",
                "Message has too many components");
        }

        // Check for common injection attempts
        String upper = message.toUpperCase();
        if (upper.contains("SCRIPT") || upper.contains("EXEC")) {
            log.warn("Potential injection attempt detected");
            throw new PokerSecurityException("INJECTION_ATTEMPT",
                "Suspicious message content");
        }
    }

    /**
     * Validates game ID format.
     *
     * @param gameId Game ID to validate
     * @return true if valid
     */
    public boolean isValidGameId(String gameId) {
        if (gameId == null || gameId.isEmpty()) {
            return false;
        }

        return gameId.matches("[a-zA-Z0-9-]{8,64}");
    }

    /**
     * Validates player name.
     *
     * @param name Player name
     * @return true if valid
     */
    public boolean isValidPlayerName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        // 2-20 characters, alphanumeric and underscores only
        return name.matches("[a-zA-Z0-9_]{2,20}");
    }
}
