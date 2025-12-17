package com.dnikitin.poker.common.exceptions;

/**
 * Thrown when a client attempts an action that violates security policies.
 * <p>
 * Indicates a "Forbidden" (403) or "Too Many Requests" (429) scenario.
 * Unlike {@link ProtocolException}, which might be a bug in the client code,
 * this exception often implies malicious intent or resource abuse.
 * </p>
 * Examples:
 * <ul>
 * <li>Injection attempts (SQL/Command).</li>
 * <li>Message flooding (Rate limit exceeded).</li>
 * <li>Sending excessively large payloads (Buffer overflow attempt).</li>
 * </ul>
 */
public class PokerSecurityException extends RuntimeException {
    private final String code;

    public PokerSecurityException(String code, String message) {
        super(message);
        this.code = code;
    }

    public PokerSecurityException(String message) {
        this("SECURITY_VIOLATION", message);
    }

    public String getCode() {
        return code;
    }
}