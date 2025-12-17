package com.dnikitin.poker.common.protocol.serverclient;

import java.util.*;

public class ServerMessageParser {

    public ServerMessage parse(String line) {
        if (line == null || line.isBlank()) {
            return new ServerMessage(ServerMessage.Type.UNKNOWN, Map.of());
        }

        String[] parts = line.trim().split("\\s+");
        if (parts.length == 0) {
            return new ServerMessage(ServerMessage.Type.UNKNOWN, Map.of());
        }

        ServerMessage.Type type;
        try {
            type = ServerMessage.Type.valueOf(parts[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            type = ServerMessage.Type.UNKNOWN;
        }


        Map<String, String> params = getParams(parts);
        return new ServerMessage(type, params);
    }


    private static Map<String, String> getParams(String[] parts) {
        Map<String, String> params = new HashMap<>();

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            int eqIndex = part.indexOf('=');
            if (eqIndex > 0) {
                String key = part.substring(0, eqIndex).toUpperCase();
                String value = part.substring(eqIndex + 1);
                params.put(key, value);
            }
        }
        return params;
    }
}
