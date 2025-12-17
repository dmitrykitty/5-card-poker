package com.dnikitin.poker.common.exceptions;


/**
 * Thrown when security violations or fraud attempts are detected.
 * Examples: player impersonation, invalid authentication, rate limit exceeded.
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
