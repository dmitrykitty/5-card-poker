package com.dnikitin.poker.common.protocol.clientserver;

import com.dnikitin.poker.common.exceptions.ProtocolException;
import com.dnikitin.poker.common.protocol.clientserver.commands.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses text-based protocol messages into Command objects.
 * Format: GAME_ID PLAYER_ID ACTION [PARAMS...]
 * or for initial commands: ACTION [PARAMS...]
 */
public class ProtocolParser {

    private static final int MAX_MESSAGE_SIZE = 512;
    private static final String PARAM_AMOUNT = "AMOUNT";
    private static final String PARAM_CARDS = "CARDS";
    private static final String PARAM_GAME = "GAME";
    private static final String PARAM_NAME = "NAME";

    /**
     * Parses a protocol line into a Command object.
     *
     * @param line The protocol line to parse
     * @return Parsed Command object
     * @throws ProtocolException if parsing fails
     */
    public Command parse(String line) {
        if (line == null || line.isBlank()) {
            throw new ProtocolException("EMPTY_MESSAGE", "Message is empty");
        }

        if (line.length() > MAX_MESSAGE_SIZE) {
            throw new ProtocolException("MESSAGE_TOO_LARGE",
                "Message exceeds maximum size of " + MAX_MESSAGE_SIZE + " bytes");
        }

        String[] parts = line.trim().split("\\s+");
        if (parts.length == 0) {
            throw new ProtocolException("EMPTY_MESSAGE", "No command found");
        }

        String firstToken = parts[0].toUpperCase();

        return switch (firstToken) {
            case "HELLO" -> parseHello(parts);
            case "CREATE" -> parseCreate(parts);
            case "JOIN" -> parseJoin(parts);
            default -> parseStandardCommand(parts);
        };
    }

    private Command parseHello(String[] parts) {
        Map<String, String> params = extractParams(parts, 1);
        String version = params.get("VERSION");
        if (version == null) {
            version = "1.0"; // default
        }
        return new HelloCommand(version);
    }

    private Command parseCreate(String[] parts) {
        Map<String, String> params = extractParams(parts, 1);
        int ante = parseIntParam(params, "ANTE", 10);
        int bet = parseIntParam(params, "BET", 10);
        String limit = params.getOrDefault("LIMIT", "FIXED");

        // Player ID will be assigned by server after CREATE
        return new CreateCommand(null, ante, bet, limit);
    }

    private Command parseJoin(String[] parts) {
        Map<String, String> params = extractParams(parts, 1);
        String gameId = params.get(PARAM_GAME);
        String name = params.get(PARAM_NAME);

        if (gameId == null || name == null) {
            throw new ProtocolException("MISSING_PARAMS", "JOIN requires GAME and NAME");
        }

        return new JoinCommand(gameId, name);
    }

    private Command parseStandardCommand(String[] parts) {
        if (parts.length < 3) {
            throw new ProtocolException("INVALID_FORMAT",
                "Standard command requires: GAME_ID PLAYER_ID ACTION [PARAMS]");
        }

        String gameId = parts[0];
        String playerId = parts[1];
        String action = parts[2].toUpperCase();

        Command.CommandType commandType;
        try {
            commandType = Command.CommandType.valueOf(action);
        } catch (IllegalArgumentException e) {
            throw new ProtocolException("UNKNOWN_COMMAND", "Unknown action: " + action);
        }

        Map<String, String> params = extractParams(parts, 3);

        return switch (commandType) {
            case BET, RAISE -> {
                int amount = parseIntParam(params, PARAM_AMOUNT, -1);
                if (amount < 0) {
                    throw new ProtocolException("MISSING_PARAMS", "BET/RAISE requires AMOUNT");
                }
                yield new BetCommand(gameId, playerId, commandType, amount);
            }
            case DRAW -> {
                String cardsStr = params.get(PARAM_CARDS);
                if (cardsStr == null) {
                    throw new ProtocolException("MISSING_PARAMS", "DRAW requires CARDS");
                }
                List<Integer> indexes = parseCardIndexes(cardsStr);
                yield new DrawCommand(gameId, playerId, indexes);
            }
            case CALL, CHECK, FOLD, START, LEAVE, STATUS, SHOW, QUIT ->
                new SimpleCommand(gameId, playerId, commandType);
            default -> throw new ProtocolException("UNSUPPORTED_COMMAND",
                "Command not implemented: " + commandType);
        };
    }

    private Map<String, String> extractParams(String[] parts, int startIndex) {
        Map<String, String> params = new HashMap<>();
        for (int i = startIndex; i < parts.length; i++) {
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

    private int parseIntParam(Map<String, String> params, String key, int defaultValue) {
        String value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ProtocolException("INVALID_PARAM",
                "Invalid integer value for " + key + ": " + value);
        }
    }

    private List<Integer> parseCardIndexes(String cardsStr) {
        if (cardsStr.equalsIgnoreCase("NONE") || cardsStr.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return Arrays.stream(cardsStr.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .toList();
        } catch (NumberFormatException e) {
            throw new ProtocolException("INVALID_PARAM",
                "Invalid card indexes: " + cardsStr);
        }
    }
}
