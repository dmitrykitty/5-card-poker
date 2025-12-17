package com.dnikitin.poker.client;

import com.dnikitin.poker.common.protocol.serverclient.ServerMessage;
import com.dnikitin.poker.common.protocol.serverclient.ServerMessageParser;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

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

    public PokerClient(String host, int port, InputStream input, PrintStream output) {
        this.host = host;
        this.port = port;
        this.gameState = new ClientGameState();
        this.ui = new ConsoleUI(input, output);
        this.parser = new ServerMessageParser();
        this.running = false;
    }

    public PokerClient(String host, int port) {
        this(host, port, System.in, System.out);
    }

    public void start() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            log.info("Connected to server at {}:{}", host, port);
            running = true;

            Thread listenerThread = Thread.ofVirtual()
                    .name("ServerListener")
                    .start(this::listenToServer);

            handleUserInput();

            listenerThread.join();

        } catch (IOException e) {
            log.error("Failed to connect: {}", e.getMessage());
            ui.printError("Connection error: " + e.getMessage());
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            disconnect();
        }
    }

    private void listenToServer() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                handleServerMessage(line);
            }
        } catch (IOException e) {
            if (running) {
                log.error("Connection to server lost: {}", e.getMessage());
                ui.printError("Connection to server lost.");
            }
        } finally {
            running = false;
        }
    }

    private void handleServerMessage(String line) {
        log.debug("Server: {}", line);
        ServerMessage msg = parser.parse(line);

        boolean showDashboard = false;

        switch (msg.type()) {
            case HELLO -> ui.printMessage("✓ Connected to server.");

            case WELCOME -> {
                String gId = msg.get("GAME").orElse(null);
                String pId = msg.get("PLAYER").orElse(null);
                String name = msg.get("NAME").orElse("player");

                if (gId != null && pId != null) {
                    gameState.setConnectionInfo(gId, pId);
                    gameState.updatePlayerInfo(pId, name, -1);
                    ui.printMessage("✓ Joined game successfully as " + name);
                } else {
                    ui.printError("Join failed. Server sent WELCOME but IDs are missing. Game: " + gId + ", Player: " + pId);
                }
            }

            case LOBBY -> {
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
                showDashboard = true;
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
                String text = msg.getDecoded("MSG");
                int amount = msg.getInt("AMOUNT", 0);

                if (amount > 0) gameState.deductChips(pId, amount);
                String name = gameState.getPlayerName(pId);
                ui.printMessage(" > " + name + ": " + type + (amount > 0 ? " " + amount : "") + (!text.isEmpty() ? " (" + text + ")" : ""));
            }

            case DEAL -> {
                if (isMe(msg.get("PLAYER").orElse(""))) {
                    msg.get("CARDS").ifPresent(gameState::updateMyHand);
                    showDashboard = true;
                }
            }

            case WINNER -> {
                String winnerId = msg.get("PLAYER").orElse("?");
                String rank = msg.getDecoded("RANK", "?");
                int pot = msg.getInt("POT");
                List<String> cards = msg.getList("CARDS");
                String winnerName = gameState.getPlayerName(winnerId);
                String displayRank = rank.contains("Fold") ? "Won by Fold" : rank;

                StringBuilder winMsg = new StringBuilder();
                winMsg.append("\n 🏆 WINNER: ").append(winnerName)
                        .append(" | ").append(displayRank)
                        .append(" | Pot: ").append(pot);
                if (!cards.isEmpty()) winMsg.append("\n    Winning Hand: ").append(String.join(", ", cards));
                winMsg.append("\n");
                ui.printMessage(winMsg.toString());

                gameState.addChips(winnerId, pot);
                gameState.setLastMessage("Winner: " + winnerName);
            }

            case OK -> {
                String message = msg.getDecoded("MESSAGE");
                if (!message.isEmpty()) ui.printMessage("✓ " + message);
            }

            case ERR -> ui.printError(msg.getDecoded("REASON", "Unknown Error"));
        }

        if (showDashboard) {
            ui.printDashboard(gameState);
            ui.printPrompt();
        }
    }

    private void handleUserInput() {
        ui.printHelp(gameState);
        ui.printPrompt();

        String input;
        while (running && (input = ui.readLine()) != null) {
            input = input.trim();
            if (input.isEmpty()) {
                ui.printPrompt();
                continue;
            }

            if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                sendCommand("QUIT");
                running = false;
                break;
            }
            if (input.equalsIgnoreCase("help")) {
                ui.printHelp(gameState);
                ui.printPrompt();
                continue;
            }
            processCommand(input);
            ui.printPrompt();
        }
    }

    private void processCommand(String input) {
        String[] parts = input.split("\\s+");
        String cmd = parts[0].toUpperCase();

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
            case "STATUS" -> sendCommand(prefix + "STATUS");
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
                String params = input.substring(input.indexOf(' ') + 1).replaceAll("\\s+", "");
                sendCommand(prefix + "DRAW CARDS=" + params);
            }
            default -> ui.printError("Unknown command. Type 'help'.");
        }
    }

    private void sendCommand(String command) {
        if (out != null) {
            out.println(command);
            log.debug("Sent: {}", command);
        }
    }

    private boolean isMe(String pId) {
        return pId != null && pId.equals(gameState.getPlayerId());
    }

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
        new PokerClient("localhost", 9999).start();
    }
}