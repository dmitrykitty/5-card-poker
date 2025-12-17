package com.dnikitin.poker.client;

import com.dnikitin.poker.common.protocol.serverclient.ServerMessage;
import com.dnikitin.poker.common.protocol.serverclient.ServerMessageParser;
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

    private final ClientGameState gameState;
    private final ConsoleUI ui;
    private final ServerMessageParser parser;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running;

    public PokerClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.gameState = new ClientGameState();
        this.ui = new ConsoleUI();
        this.parser = new ServerMessageParser();
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

            // Start listener thread
            Thread listenerThread = Thread.ofVirtual()
                    .name("ServerListener")
                    .start(this::listenToServer);

            // Handle user input in main thread
            handleUserInput();

            listenerThread.join();

        } catch (IOException e) {
            log.error("Failed to connect: {}", e.getMessage());
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
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
    private void handleServerMessage(String line) {
        log.debug("Server: {}", line);
        ServerMessage msg = parser.parse(line);

        boolean showDashboard = false;

        switch (msg.type()) {
            case HELLO -> ui.printMessage("✓ Connected to server.");

            case WELCOME -> {
                String gId = msg.get("GAME").orElse(null);
                String pId = msg.get("PLAYER").orElse(null);

                if (gId != null && pId != null) {
                    gameState.setConnectionInfo(gId, pId);
                    ui.printMessage("✓ Joined game successfully.");
                }
            }

            case LOBBY -> {
                // Zapamiętujemy imię i żetony gracza (siebie i innych)
                String pId = msg.get("PLAYER").orElse(null);
                String name = msg.get("NAME").orElse("Unknown");
                int chips = msg.getInt("CHIPS", -1);

                // Jeśli serwer w PLAYER wysyła imię zamiast ID (stara wersja), to mapowanie może nie działać idealnie,
                // ale zakładając poprawny protokół:
                if (pId != null) {
                    gameState.updatePlayerInfo(pId, name, chips);
                }

                ui.printMessage(" [LOBBY] " + name + (chips >= 0 ? " (" + chips + " chips)" : ""));;
            }

            case STARTED -> {
                gameState.setLastMessage("Game Started!");
            }

            case STATE -> {
                String phase = msg.get("PHASE").orElse("UNKNOWN");
                gameState.updatePhase(phase);

                ui.printMessage(" --- PHASE: " + phase + " ---");
            }

            case ROUND -> {
                int pot = msg.getInt("POT", gameState.getCurrentPot());
                int highest = msg.getInt("HIGHESTBET", 0);
                gameState.updateRoundInfo(pot, highest);
            }

            case TURN -> {
                String activePlayer = msg.get("PLAYER").orElse("");

                if (isMe(activePlayer)) {
                    gameState.setLastMessage(">>> YOUR TURN! <<<");
                    showDashboard = true;
                } else {
                    String opponent = gameState.getPlayerName(activePlayer);
                    ui.printMessage(" Waiting for " + opponent + "...");
                }
            }

            case ACTION -> {
                String pId = msg.get("PLAYER").orElse("?");
                String type = msg.get("TYPE").orElse("?");
                String text = msg.get("MSG").orElse("");
                int amount = msg.getInt("AMOUNT", 0);

                // Aktualizujemy lokalnie żetony i zakłady
                if (amount > 0) {
                    gameState.deductChips(pId, amount);
                }
                // Jeśli fold, to fold

                String name = gameState.getPlayerName(pId);
                ui.printMessage(" > " + name + ": " + type + (amount > 0 ? " " + amount : "") + " (" + text + ")");

                // Jeśli to JA wykonałem akcję (np. CALL/CHECK), pokaż dashboard na chwilę jako potwierdzenie stanu
                if (isMe(pId)) {
                    // showDashboard = true; // Opcjonalne: wyłączam, żebyś nie widział go 2 razy (raz przy akcji, raz przy nastepnej turze)
                }
            }

            case DEAL -> {
                // Aktualizujemy rękę po cichu
                if (isMe(msg.get("PLAYER").orElse(""))) {
                    msg.get("CARDS").ifPresent(gameState::updateMyHand);
                    // Tutaj MOŻNA narysować dashboard, bo dostałeś karty
                    showDashboard = true;
                }
            }

            case WINNER -> {
                String winnerId = msg.get("PLAYER").orElse("?");
                String rank = msg.get("RANK").orElse("?");
                String potStr = msg.get("POT").orElse("0");
                String cardsStr = msg.get("CARDS").orElse("");

                int pot = Integer.parseInt(potStr);
                String winnerName = gameState.getPlayerName(winnerId);
                String displayRank = rank.contains("Fold") ? "Won by Fold" : rank.replace("_", " ");

                StringBuilder winMsg = new StringBuilder();
                winMsg.append("\n 🏆 WINNER: ").append(winnerName)
                        .append(" | ").append(displayRank)
                        .append(" | Pot: ").append(pot);

                if (!cardsStr.isEmpty() && !cardsStr.equals("NONE")) {
                    winMsg.append("\n    Winning Hand: ").append(cardsStr);
                }

                winMsg.append("\n");

                ui.printMessage(winMsg.toString());

                gameState.addChips(winnerId, pot);
                gameState.setLastMessage("Winner: " + winnerName);
            }

            case OK -> msg.get("MESSAGE").ifPresent(m -> ui.printMessage("✓ " + m));

            case ERR -> ui.printError(msg.get("REASON").orElse("Unknown Error"));
        }

        if (showDashboard) {
            ui.printDashboard(gameState);
        }
    }

    /**
     * Handles user input from console.
     */
    private void handleUserInput() {
        try (Scanner scanner = new Scanner(System.in)) {
            ui.printHelp(gameState);

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
                    ui.printHelp(gameState);
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

        // Commands that don't need game/player ID
        if (cmd.equals("CREATE")) {
            sendCommand("CREATE ANTE=10 BET=10 LIMIT=FIXED");
            return;
        }
        if (cmd.equals("JOIN")) {
            if (parts.length < 3) {
                ui.printError("Usage: join <gameId> <name>");
                return;
            }
            sendCommand("JOIN GAME=" + parts[1] + " NAME=" + parts[2]);
            return;
        }

        // --- In-Game Commands Validation ---
        if (gameState.getGameId() == null || gameState.getPlayerId() == null) {
            ui.printError("You must join a game first.");
            return;
        }

        String prefix = gameState.getGameId() + " " + gameState.getPlayerId() + " ";

        switch (cmd) {
            case "START" -> sendCommand(prefix + "START");
            case "CALL" -> sendCommand(prefix + "CALL");
            case "CHECK" -> sendCommand(prefix + "CHECK");
            case "FOLD" -> sendCommand(prefix + "FOLD");
            case "RAISE" -> {
                if (parts.length < 2) {
                    ui.printError("Usage: raise <amount>");
                    return;
                }
                sendCommand(prefix + "RAISE AMOUNT=" + parts[1]);
            }
            case "DRAW" -> {
                if (parts.length < 2) {
                    ui.printError("Usage: draw <indexes> (e.g., 0,2,4 or NONE)");
                    return;
                }
                sendCommand(prefix + "DRAW CARDS=" + parts[1]);
            }
            default -> ui.printError("Unknown command. Type 'help'.");
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

    private boolean isMe(String pId) {
        return pId != null && pId.equals(gameState.getPlayerId());
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
        new PokerClient("localhost", 7777).start();
    }
}
