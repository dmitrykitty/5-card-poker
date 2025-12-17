package com.dnikitin.poker.common.protocol.serverclient;

import java.util.*;

public record ServerMessage(Type type, Map<String, String> params) {
    public enum Type {
        HELLO, WELCOME, LOBBY, STARTED, STATE, TURN, ACTION,
        DEAL, WINNER, ROUND, OK, ERR, UNKNOWN
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(params.get(key));
    }

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

    public int getInt(String key) {
        return getInt(key, 0);
    }

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

    public List<String> getList(String key) {
        return get(key)
                .filter(s -> !s.equals("NONE") && !s.isEmpty())
                .map(s -> Arrays.asList(s.split(",")))
                .orElse(Collections.emptyList());
    }


}
