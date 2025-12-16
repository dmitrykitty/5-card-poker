package com.dnikitin.poker.common.protocol.serverclient;

import java.util.Map;
import java.util.Optional;

public record ServerMessage(Type type, Map<String, String> params) {
    public enum Type {
        HELLO, WELCOME, LOBBY, STARTED, STATE, TURN, ACTION,
        DEAL, WINNER, ROUND, OK, ERR, UNKNOWN
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(params.get(key));
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(params.getOrDefault(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
