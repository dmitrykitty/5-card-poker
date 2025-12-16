package com.dnikitin.poker.common.exceptions;

import lombok.Getter;

/**
 * Thrown when security violations or fraud attempts are detected.
 * Examples: player impersonation, invalid authentication, rate limit exceeded.
 */
@Getter
public class PokerSecurityException extends RuntimeException {
    private final String code;

    public PokerSecurityException(String code, String message) {
        super(message);
        this.code = code;
    }

    public PokerSecurityException(String message) {
        this("SECURITY_VIOLATION", message);
    }

}
