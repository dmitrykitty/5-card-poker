package com.dnikitin.poker.common.exceptions;

/**
 * Thrown when protocol parsing or validation fails.
 * Examples: malformed messages, unknown commands, invalid parameters.
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
