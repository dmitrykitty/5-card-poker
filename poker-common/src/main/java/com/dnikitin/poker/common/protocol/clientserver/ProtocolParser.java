package com.dnikitin.poker.common.protocol.clientserver;

import com.dnikitin.poker.common.exceptions.ProtocolException;
import com.dnikitin.poker.common.protocol.clientserver.commands.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsible for parsing raw text-based network messages into typed {@link Command} objects.
 * <p>
 * <b>Protocol Format:</b>
 * The protocol uses a space-delimited format with Key-Value pairs for parameters.
 * <br>
 * Standard format: <code>GAME_ID PLAYER_ID ACTION [PARAM=VALUE]...</code>
 * <br>
 * Initial format: <code>ACTION [PARAM=VALUE]...</code> (e.g., HELLO, CREATE, JOIN)
 * </p>
 * <p>
 * This class implements strict validation to ensure messages conform to size limits
 * and expected formats before they reach the game logic.
 * </p>
 */
public class ProtocolParser {

    private static final int MAX_MESSAGE_SIZE = 512;
    private static final String PARAM_AMOUNT = "AMOUNT";
    private static final String PARAM_CARDS = "CARDS";
    private static final String PARAM_GAME = "GAME";
    private static final String PARAM_NAME = "NAME";

    /**
     * Parses a raw protocol string into a specific Command implementation.
     *
     * @param line The raw string received from the socket (excluding newline characters).
     * @return A concrete {@link Command} object (e.g., {@link BetCommand}, {@link JoinCommand}).
     * @throws ProtocolException If the message is empty, too large, malformed, or contains unknown commands.
     */
    public Command parse(String line) {
        if (line == null || line.isBlank()) {
            throw new ProtocolException("EMPTY_MESSAGE", "Message is empty");
        }

        // Security: Fail fast if message exceeds buffer limits
        if (line.length() > MAX_MESSAGE_SIZE) {
            throw new ProtocolException("MESSAGE_TOO_LARGE",
                    "Message exceeds maximum size of " + MAX_MESSAGE_SIZE + " bytes");
        }

        String[] parts = line.trim().split("\\s+");
        if (parts.length == 0) {
            throw new ProtocolException("EMPTY_MESSAGE", "No command found");
        }

        String firstToken = parts[0].toUpperCase();

        // Switch expression handles both initial handshake commands and in-game commands
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
        // Standard commands must have context: GameID and PlayerID
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
            // Simple commands carry no extra parameters
            case CALL, CHECK, FOLD, START, LEAVE, STATUS, SHOW, QUIT ->
                    new SimpleCommand(gameId, playerId, commandType);
            default -> throw new ProtocolException("UNSUPPORTED_COMMAND",
                    "Command not implemented: " + commandType);
        };
    }

    /**
     * Helper to extract Key=Value pairs from the command string parts.
     */
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