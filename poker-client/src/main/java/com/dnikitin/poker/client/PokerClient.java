package com.dnikitin.poker.client;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * Console-based poker client.
 * Connects to the poker server and provides a text-based UI.
 */
@Slf4j
public class PokerClient {
    private final String host;
    private final int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running;

    private String playerId;
    private String gameId;

    public PokerClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.running = false;
    }

    /**
     * Connects to the server and starts the client.
     */
    public void start() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            log.info("Connected to server at {}:{}", host, port);
            running = true;

            // Start thread to listen for server messages
            Thread listenerThread = Thread.ofVirtual()
                .name("ServerListener")
                .start(this::listenToServer);

            // Main thread handles user input
            handleUserInput();

            listenerThread.join();

        } catch (IOException e) {
            log.error("Failed to connect to server: {}", e.getMessage());
        } catch (InterruptedException e) {
            log.error("Client interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            disconnect();
        }
    }

    /**
     * Listens for messages from the server.
     */
    private void listenToServer() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                handleServerMessage(line);
            }
        } catch (IOException e) {
            if (running) {
                log.error("Connection to server lost: {}", e.getMessage());
            }
        } finally {
            running = false;
        }
    }

    /**
     * Handles messages received from the server.
     */
    private void handleServerMessage(String message) {
        log.debug("Server: {}", message);

        String[] parts = message.split("\\s+");
        if (parts.length == 0) return;

        String command = parts[0];

        switch (command) {
            case "HELLO" -> System.out.println("✓ Connected to poker server");

            case "WELCOME" -> {
                extractParam(parts, "PLAYER").ifPresent(id -> {
                    playerId = id;
                    System.out.println("✓ Joined game! Your Player ID: " + id);
                });
                extractParam(parts, "GAME").ifPresent(id -> gameId = id);
            }

            case "LOBBY" -> {
                System.out.println("═══ LOBBY ═══");
                extractParam(parts, "PLAYER").ifPresent(name ->
                    System.out.println("  Player: " + name));
                extractParam(parts, "CHIPS").ifPresent(chips ->
                    System.out.println("  Chips: " + chips));
            }

            case "STARTED" -> System.out.println("\n▶ GAME STARTED!\n");

            case "STATE" -> extractParam(parts, "PHASE").ifPresent(phase ->
                System.out.println("═══ Phase: " + phase + " ═══"));

            case "TURN" -> extractParam(parts, "PLAYER").ifPresent(player -> {
                if (player.equals(playerId)) {
                    System.out.println("\n>>> YOUR TURN! <<<");
                    extractParam(parts, "PHASE").ifPresent(phase ->
                        System.out.println("Phase: " + phase));
                    extractParam(parts, "CALL").ifPresent(call ->
                        System.out.println("To call: " + call));
                } else {
                    System.out.println("Waiting for player " + player + "...");
                }
            });

            case "ACTION" -> {
                String player = extractParam(parts, "PLAYER").orElse("?");
                String type = extractParam(parts, "TYPE").orElse("?");
                String msg = extractParam(parts, "MSG").orElse("");
                System.out.println("  → " + player + ": " + type + " " + msg);
            }

            case "DEAL" -> extractParam(parts, "CARDS").ifPresent(cards -> {
                if (!cards.equals("HIDDEN")) {
                    System.out.println("\n┌─ YOUR CARDS ─┐");
                    System.out.println("│ " + cards + " │");
                    System.out.println("└──────────────┘");
                }
            });

            case "WINNER" -> {
                String winner = extractParam(parts, "PLAYER").orElse("?");
                String pot = extractParam(parts, "POT").orElse("?");
                String rank = extractParam(parts, "RANK").orElse("?");
                System.out.println("\n★ WINNER: " + winner + " ★");
                System.out.println("  Pot: " + pot + " | Hand: " + rank);
            }

            case "OK" -> extractParam(parts, "MESSAGE").ifPresent(msg ->
                System.out.println("✓ " + msg));

            case "ERR" -> {
                String code = extractParam(parts, "CODE").orElse("ERROR");
                String reason = extractParam(parts, "REASON").orElse("Unknown");
                System.out.println("✗ Error [" + code + "]: " + reason);
            }

            default -> System.out.println("Server: " + message);
        }
    }

    /**
     * Handles user input from console.
     */
    private void handleUserInput() {
        try (Scanner scanner = new Scanner(System.in)) {
            printHelp();

            while (running) {
                System.out.print("\n> ");
                if (!scanner.hasNextLine()) break;

                String input = scanner.nextLine().trim();
                if (input.isEmpty()) continue;

                if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                    sendCommand("QUIT");
                    running = false;
                    break;
                }

                if (input.equalsIgnoreCase("help")) {
                    printHelp();
                    continue;
                }

                processCommand(input);
            }
        }
    }

    /**
     * Processes user commands.
     */
    private void processCommand(String input) {
        String[] parts = input.split("\\s+");
        String cmd = parts[0].toUpperCase();

        switch (cmd) {
            case "CREATE" -> sendCommand("CREATE ANTE=10 BET=10 LIMIT=FIXED");
            case "JOIN" -> {
                if (parts.length < 3) {
                    System.out.println("Usage: join <gameId> <name>");
                    return;
                }
                sendCommand("JOIN GAME=" + parts[1] + " NAME=" + parts[2]);
            }
            case "START" -> sendCommand(gameId + " " + playerId + " START");
            case "CALL" -> sendCommand(gameId + " " + playerId + " CALL");
            case "CHECK" -> sendCommand(gameId + " " + playerId + " CHECK");
            case "FOLD" -> sendCommand(gameId + " " + playerId + " FOLD");
            case "RAISE" -> {
                if (parts.length < 2) {
                    System.out.println("Usage: raise <amount>");
                    return;
                }
                sendCommand(gameId + " " + playerId + " RAISE AMOUNT=" + parts[1]);
            }
            case "DRAW" -> {
                if (parts.length < 2) {
                    System.out.println("Usage: draw <indexes> (e.g., draw 0,2,4 or draw NONE)");
                    return;
                }
                sendCommand(gameId + " " + playerId + " DRAW CARDS=" + parts[1]);
            }
            default -> System.out.println("Unknown command. Type 'help' for available commands.");
        }
    }

    /**
     * Sends a command to the server.
     */
    private void sendCommand(String command) {
        if (out != null) {
            out.println(command);
            log.debug("Sent: {}", command);
        }
    }

    /**
     * Extracts a parameter value from server message.
     */
    private java.util.Optional<String> extractParam(String[] parts, String key) {
        String prefix = key + "=";
        for (String part : parts) {
            if (part.startsWith(prefix)) {
                return java.util.Optional.of(part.substring(prefix.length()));
            }
        }
        return java.util.Optional.empty();
    }

    /**
     * Prints help information.
     */
    private void printHelp() {
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("       5-CARD DRAW POKER CLIENT");
        System.out.println("═══════════════════════════════════════");
        System.out.println("Commands:");
        System.out.println("  create              - Create a new game");
        System.out.println("  join <id> <name>    - Join game with name");
        System.out.println("  start               - Start the game");
        System.out.println("  call                - Call current bet");
        System.out.println("  check               - Check (no bet)");
        System.out.println("  fold                - Fold hand");
        System.out.println("  raise <amount>      - Raise bet");
        System.out.println("  draw <indexes>      - Exchange cards (0-4)");
        System.out.println("  help                - Show this help");
        System.out.println("  quit                - Disconnect");
        System.out.println("═══════════════════════════════════════\n");
    }

    /**
     * Disconnects from the server.
     */
    private void disconnect() {
        running = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
            log.info("Disconnected from server");
        } catch (IOException e) {
            log.error("Error during disconnect", e);
        }
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 7777;

        PokerClient client = new PokerClient(host, port);
        client.start();
    }
}
