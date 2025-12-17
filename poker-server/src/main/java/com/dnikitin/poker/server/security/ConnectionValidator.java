package com.dnikitin.poker.server.security;

import com.dnikitin.poker.common.exceptions.ProtocolException;
import com.dnikitin.poker.common.exceptions.PokerSecurityException;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides security validation for incoming network traffic.
 * <p>
 * This class implements a defense-in-depth strategy to protect the server from common attacks:
 * <ul>
 * <li><b>Buffer Overflow / Memory Exhaustion:</b> Limits message size and complexity.</li>
 * <li><b>Injection Attacks:</b> Scans for suspicious patterns (e.g., null bytes, script tags).</li>
 * <li><b>Protocol Compliance:</b> Ensures IDs and names match expected formats (regex).</li>
 * </ul>
 * </p>
 */
@Slf4j
public class ConnectionValidator {
    private static final int MAX_MESSAGE_SIZE = 512;
    private static final int MAX_COMMAND_PARTS = 20;

    /**
     * Validates a raw protocol message string before it is parsed.
     *
     * @param message The raw message received from the socket.
     * @throws PokerSecurityException if the message violates security policies (e.g., too long, contains null bytes).
     * @throws ProtocolException if the message is structurally invalid (e.g., null).
     */
    public void validateMessage(String message) {
        if (message == null) {
            throw new ProtocolException("NULL_MESSAGE", "Message is null");
        }

        // Check message size to prevent memory exhaustion
        if (message.length() > MAX_MESSAGE_SIZE) {
            log.warn("Message too large: {} bytes", message.length());
            throw new PokerSecurityException("MESSAGE_TOO_LARGE",
                    String.format("Message exceeds %d bytes", MAX_MESSAGE_SIZE));
        }

        // Check for suspicious characters often used in binary injection
        if (message.contains("\0") || message.contains("\r\n")) {
            log.warn("Suspicious characters in message");
            throw new PokerSecurityException("INVALID_CHARACTERS",
                    "Message contains invalid characters");
        }

        // Check number of parts to prevent CPU exhaustion during parsing (Regex DoS)
        String[] parts = message.split("\\s+");
        if (parts.length > MAX_COMMAND_PARTS) {
            log.warn("Too many command parts: {}", parts.length);
            throw new PokerSecurityException("TOO_MANY_PARTS",
                    "Message has too many components");
        }

        // Simple heuristic to block common command injection keywords
        String upper = message.toUpperCase();
        if (upper.contains("SCRIPT") || upper.contains("EXEC")) {
            log.warn("Potential injection attempt detected");
            throw new PokerSecurityException("INJECTION_ATTEMPT",
                    "Suspicious message content");
        }
    }

    /**
     * Validates the format of a game ID (e.g., UUID or structured string).
     *
     * @param gameId The Game ID to check.
     * @return {@code true} if valid (alphanumeric, dashes, length 8-64), {@code false} otherwise.
     */
    public boolean isValidGameId(String gameId) {
        if (gameId == null || gameId.isEmpty()) {
            return false;
        }

        return gameId.matches("[a-zA-Z0-9-]{8,64}");
    }

    /**
     * Validates a player name against strict rules to prevent UI spoofing or formatting issues.
     *
     * @param name The player name.
     * @return {@code true} if valid (2-20 chars, alphanumeric/underscore), {@code false} otherwise.
     */
    public boolean isValidPlayerName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        // 2-20 characters, alphanumeric and underscores only
        return name.matches("[a-zA-Z0-9_]{2,20}");
    }
}