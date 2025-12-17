package com.dnikitin.poker.client;

import com.dnikitin.poker.common.protocol.serverclient.ServerMessage;
import com.dnikitin.poker.common.protocol.serverclient.ServerMessageParser;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
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
                // Użycie getInt jest bezpieczne
                String pId = msg.get("PLAYER").orElse(null);
                String name = msg.get("NAME").orElse("Unknown");
                int chips = msg.getInt("CHIPS", -1);

                if (pId != null) {
                    gameState.updatePlayerInfo(pId, name, chips);
                }

                ui.printMessage(" [LOBBY] " + name + (chips >= 0 ? " (" + chips + " chips)" : ""));
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
                // Użycie getInt jest bezpieczne
                int pot = msg.getInt("POT", gameState.getCurrentPot());
                int highest = msg.getInt("HIGHESTBET", 0);
                gameState.updateRoundInfo(pot, highest);
            }

            case TURN -> {
                String activePlayer = msg.get("PLAYER").orElse("");

                if (isMe(activePlayer)) {
                    // Tutaj możesz odczytać CALL i MINRAISE, które są bezpiecznymi intami
                    int call = msg.getInt("CALL");
                    int minRaise = msg.getInt("MINRAISE");

                    gameState.setLastMessage(String.format(">>> YOUR TURN! (Call: %d, MinRaise: %d) <<<", call, minRaise));
                    showDashboard = true;
                } else {
                    String opponent = gameState.getPlayerName(activePlayer);
                    ui.printMessage(" Waiting for " + opponent + "...");
                }
            }

            case ACTION -> {
                String pId = msg.get("PLAYER").orElse("?");
                String type = msg.get("TYPE").orElse("?");

                // Użycie getDecoded() do wyczyszczenia wiadomości z '_'
                String text = msg.getDecoded("MSG");
                int amount = msg.getInt("AMOUNT", 0);

                // Aktualizujemy lokalnie żetony i zakłady
                if (amount > 0) {
                    gameState.deductChips(pId, amount);
                }

                String name = gameState.getPlayerName(pId);
                ui.printMessage(" > " + name + ": " + type + (amount > 0 ? " " + amount : "") + (!text.isEmpty() ? " (" + text + ")" : ""));
            }

            case DEAL -> {
                // Aktualizujemy rękę po cichu
                if (isMe(msg.get("PLAYER").orElse(""))) {
                    msg.get("CARDS").ifPresent(gameState::updateMyHand);
                    showDashboard = true;
                }
            }

            case WINNER -> {
                String winnerId = msg.get("PLAYER").orElse("?");
                // Użycie getDecoded() dla czystego rankingu
                String rank = msg.getDecoded("RANK", "?");
                // Użycie getInt() dla bezpiecznego potu
                int pot = msg.getInt("POT");
                // Użycie getList() dla czystej listy kart
                List<String> cards = msg.getList("CARDS");

                String winnerName = gameState.getPlayerName(winnerId);
                // getDecoded już usunął _, więc wystarczy tylko obsłużyć "Fold"
                String displayRank = rank.contains("Fold") ? "Won by Fold" : rank;

                StringBuilder winMsg = new StringBuilder();
                winMsg.append("\n 🏆 WINNER: ").append(winnerName)
                        .append(" | ").append(displayRank)
                        .append(" | Pot: ").append(pot);

                // Sprawdzamy, czy lista kart nie jest pusta
                if (!cards.isEmpty()) {
                    winMsg.append("\n    Winning Hand: ").append(String.join(", ", cards));
                }

                winMsg.append("\n");

                ui.printMessage(winMsg.toString());

                gameState.addChips(winnerId, pot);
                gameState.setLastMessage("Winner: " + winnerName);
            }

            case OK -> {
                // Użycie getDecoded() dla czystego komunikatu
                String message = msg.getDecoded("MESSAGE");
                if (!message.isEmpty()) {
                    ui.printMessage("✓ " + message);
                }
            }

            case ERR -> {
                // Użycie getDecoded() dla czystego komunikatu o błędzie
                ui.printError(msg.getDecoded("REASON", "Unknown Error"));
            }
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