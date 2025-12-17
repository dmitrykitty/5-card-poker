package com.dnikitin.poker.common.exceptions;

/**
 * Thrown when an incoming message fails structural validation.
 * <p>
 * Indicates a "Bad Request" (Client Error 400 equivalent).
 * Causes:
 * <ul>
 * <li>Malformed syntax (missing parameters).</li>
 * <li>Unknown command verb.</li>
 * <li>Data type mismatch (expected int, got string).</li>
 * </ul>
 * </p>
 */
public class ProtocolException extends RuntimeException {
    private final String code;

    public ProtocolException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ProtocolException(String message) {
        this("PROTOCOL_ERROR", message);
    }

    public String getCode() {
        return code;
    }
}