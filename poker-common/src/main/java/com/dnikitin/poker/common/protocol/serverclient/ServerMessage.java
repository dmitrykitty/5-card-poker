package com.dnikitin.poker.common.protocol.serverclient;

import java.util.*;

/**
 * A structured representation of a message received from the Server.
 * <p>
 * This class is a <b>Record</b>, ensuring immutability of the message data.
 * It is produced by the client-side parser after reading a line from the server.
 * </p>
 *
 * @param type   The type of the message (e.g., DEAL, TURN).
 * @param params A map of Key-Value parameters extracted from the message string.
 */
public record ServerMessage(Type type, Map<String, String> params) {

    /**
     * Enum defining all known message types sent by the server.
     */
    public enum Type {
        HELLO, WELCOME, LOBBY, STARTED, STATE, TURN, ACTION,
        DEAL, WINNER, ROUND, OK, ERR, UNKNOWN
    }

    /**
     * Retrieves a parameter value safely.
     *
     * @param key The parameter key (e.g., "PLAYER").
     * @return An Optional containing the value, or empty if missing.
     */
    public Optional<String> get(String key) {
        return Optional.ofNullable(params.get(key));
    }

    /**
     * Retrieves a parameter as an integer, with a fallback default.
     *
     * @param key          The parameter key.
     * @param defaultValue Value to return if key is missing or not a valid number.
     * @return The parsed integer or the default value.
     */
    public int getInt(String key, int defaultValue) {
        return get(key)
                .map(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    /**
     * Retrieves a parameter as an integer, defaulting to 0 if missing.
     */
    public int getInt(String key) {
        return getInt(key, 0);
    }

    /**
     * Retrieves a string parameter and decodes it (replaces underscores with spaces).
     * Useful for chat messages or reasons.
     *
     * @param key The parameter key.
     * @return The decoded string.
     */
    public String getDecoded(String key) {
        return get(key)
                .map(s -> s.replace("_", " "))
                .orElse("");
    }

    public String getDecoded(String key, String defaultValue) {
        return get(key)
                .map(s -> s.replace("_", " "))
                .orElse(defaultValue);
    }

    /**
     * Parses a parameter expected to be a comma-separated list.
     *
     * @param key The parameter key (e.g., "CARDS").
     * @return A list of strings, or an empty list if missing/NONE.
     */
    public List<String> getList(String key) {
        return get(key)
                .filter(s -> !s.equals("NONE") && !s.isEmpty())
                .map(s -> Arrays.asList(s.split(",")))
                .orElse(Collections.emptyList());
    }
}