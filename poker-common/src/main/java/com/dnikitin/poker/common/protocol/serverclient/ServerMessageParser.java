package com.dnikitin.poker.common.protocol.serverclient;

import java.util.*;

/**
 * Parsers text messages received FROM the Server (Client-side parser).
 * <p>
 * Unlike {@link com.dnikitin.poker.common.protocol.clientserver.ProtocolParser}, which runs on the server
 * to interpret user commands, this class runs on the <b>Client</b> to interpret state updates
 * sent by the server.
 * </p>
 * <p>
 * It converts raw strings like <code>"DEAL PLAYER=123 CARDS=Ah,Ks"</code> into structured
 * {@link ServerMessage} objects.
 * </p>
 */
public class ServerMessageParser {

    /**
     * Parses a single line of server output.
     *
     * @param line The raw protocol string.
     * @return A {@link ServerMessage} object containing the type and extracted parameters.
     * Returns a message of type UNKNOWN if the line is empty or malformed.
     */
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

    /**
     * Extracts Key=Value pairs from the message tokens.
     * Starts from index 1, as index 0 is the message type.
     */
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