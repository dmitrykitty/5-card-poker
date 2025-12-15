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

        boolean shouldRepaint = false;

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
                String pId = msg.get("PLAYER").orElse(null); // Serwer musi wysyłać ID w LOBBY!
                // Uwaga: Jeśli Twój obecny serwer wysyła "LOBBY PLAYER=Name", a nie ID,
                // to trzeba poprawić serwer lub zgadywać.
                // Zakładam, że protokół to: LOBBY PLAYER=<uuid> CHIPS=<n> NAME=<name>

                String name = msg.get("NAME").orElse("Unknown");
                int chips = msg.getInt("CHIPS", -1);

                // Jeśli serwer w PLAYER wysyła imię zamiast ID (stara wersja), to mapowanie może nie działać idealnie,
                // ale zakładając poprawny protokół:
                if (pId != null) {
                    gameState.updatePlayerInfo(pId, name, chips);
                }

                ui.printMessage(" [LOBBY] " + name + " (" + chips + " chips)");
            }

            case STARTED -> {
                gameState.setLastMessage("Game Started!");
                shouldRepaint = true;
            }

            case STATE -> {
                String phase = msg.get("PHASE").orElse(gameState.getCurrentPhase());
                gameState.updateTurn(phase, gameState.getAmountToCall());
                shouldRepaint = true;
            }

            case ROUND -> {
                int pot = msg.getInt("POT", gameState.getCurrentPot());
                gameState.updateRoundInfo(pot);
                shouldRepaint = true;
            }

            case TURN -> {
                String activePlayerId = msg.get("PLAYER").orElse("");
                String phase = msg.get("PHASE").orElse(gameState.getCurrentPhase());
                int toCall = msg.getInt("CALL", 0);

                gameState.updateTurn(phase, toCall);

                if (isMe(activePlayerId)) {
                    gameState.setLastMessage(">>> YOUR TURN! <<<");
                    ui.printDashboard(gameState);
                    shouldRepaint = false;
                } else {
                    // Wyświetlamy imię zamiast ID
                    String opponentName = gameState.getPlayerName(activePlayerId);
                    gameState.setLastMessage("Waiting for " + opponentName + "...");
                    shouldRepaint = true;
                }
            }

            case ACTION -> {
                String pId = msg.get("PLAYER").orElse("?");
                String type = msg.get("TYPE").orElse("?");
                String text = msg.get("MSG").orElse("");
                int amount = msg.getInt("AMOUNT", 0);

                // --- POPRAWKA 1: Logika Żetonów ---
                // Jeśli akcja wiąże się z wydaniem kasy, aktualizujemy stan lokalny
                if (amount > 0 && (type.equals("ANTE") || type.equals("BET") || type.equals("RAISE") || type.equals("CALL"))) {
                    gameState.deductChips(pId, amount);
                    // Jeśli to ja, musimy przerysować dashboard
                    if (isMe(pId)) shouldRepaint = true;
                }

                // --- POPRAWKA 2: Wyświetlanie Imienia ---
                String displayName = gameState.getPlayerName(pId);
                ui.printMessage(" > " + displayName + ": " + type + " (" + text + ")");
            }

            case DEAL -> {
                if (isMe(msg.get("PLAYER").orElse(""))) {
                    msg.get("CARDS").ifPresent(gameState::updateMyHand);
                    shouldRepaint = true;
                }
            }

            case WINNER -> {
                String winnerId = msg.get("PLAYER").orElse("?");
                String rank = msg.get("RANK").orElse("?");
                String pot = msg.get("POT").orElse("0");

                String winnerName = gameState.getPlayerName(winnerId);

                // --- POPRAWKA 3: Formatowanie ---
                // Zamiana podkreśleń na spacje w rankingu (np. Opponents_Folded -> Opponents Folded)
                String displayRank = rank.replace("_", " ");

                ui.printMessage("\n ★ WINNER: " + winnerName +
                        " | " + displayRank + " | Pot: " + pot + "\n");

                // Zwycięzca zgarnia pulę (opcjonalna aktualizacja lokalna dla efektu)
                gameState.addChips(winnerId, Integer.parseInt(pot));

                gameState.setLastMessage("Hand Finished. Winner: " + winnerName);
                shouldRepaint = true;
            }

            case OK -> msg.get("MESSAGE").ifPresent(m -> ui.printMessage("✓ " + m));

            case ERR -> ui.printError(msg.get("REASON").orElse("Unknown Error"));
        }

        if (shouldRepaint) {
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
