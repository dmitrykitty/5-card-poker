package com.dnikitin.poker.server;

import com.dnikitin.poker.common.model.events.GameEvent;
import com.dnikitin.poker.common.model.events.GameObserver;
import com.dnikitin.poker.game.GameManager;
import com.dnikitin.poker.game.Player;
import com.dnikitin.poker.game.Table;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ClientHandler implements Runnable, GameObserver {

    private final SocketChannel socketChannel;
    private PrintWriter out;

    private Player player;
    private Table table;


    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(Channels.newReader(socketChannel, StandardCharsets.UTF_8));
             PrintWriter printWriter = new PrintWriter(Channels.newWriter(socketChannel, StandardCharsets.UTF_8),
                     true)) {
            out = printWriter;
            out.println("HELLO VERSION=1.0");

            for (String line; (line = in.readLine()) != null; ) {
                handleCommand(line.trim());
            }
        } catch (IOException e) {
            log.info("Client disconnected: {}", e.getMessage());
        } finally {
            handleDisconnect();
        }

    }

    @Override
    public void onGameEvent(GameEvent event) {
        String messageToSend = convertEventProtocol(event);
        if (messageToSend != null) {
            out.println(messageToSend);
        }
    }

    private String convertEventProtocol(GameEvent event) {
        return switch (event) {
            case GameEvent.PlayerJoined pj -> "LOBBY_UPDATE PLAYER=" + pj.name() + " CHIPS=" + pj.chips();
            case GameEvent.GameStarted gs -> "STARTED GAME=" + gs.gameId();
            case GameEvent.TurnChanged tc -> "TURN PLAYER=" + tc.activePlayerId();
            case GameEvent.PlayerAction pa ->
                    "ACTION PLAYER=" + pa.playerId() + " TYPE=" + pa.actionType() + " MSG=" + pa.message();
            case GameEvent.GameFinished gf ->
                    "WINNER PLAYER=" + gf.winnerId() + " POT=" + gf.potAmount() + " RANK=" + gf.handRank();
            case GameEvent.StateChanged sc -> "STATE NEW=" + sc.newState();
            case GameEvent.CardsDealt cd -> {
                if (player != null && cd.playerId().equals(player.getId())) {
                    yield "DEAL CARDS=" + cd.cards();
                } else {
                    yield "DEAL PLAYER=" + cd.playerId() + " CARDS=HIDDEN";
                }
            }
            default -> null;
        };
    }

    private void handleCommand(String line) {
        log.debug("Received: {}", line);
        String[] parts = line.split("\\s+");
        String command = parts[0].toUpperCase();

        try {
            switch (command) {
                case "CREATE" -> handleCreate();
                case "JOIN" -> handleJoin(parts);
                case "START" -> handleStart();
                case "FOLD" -> handleFold();
                case "CHECK" -> handleCheck();
                case "CALL" -> handleCall();
                case "RAISE" -> handleRaise(parts);
                case "DRAW" -> handleDraw(parts);
                case "QUIT" -> throw new IOException("Client requested quit");
                default -> out.println("ERR REASON=UNKNOWN_COMMAND");
            }
        } catch (Exception e) {
            log.error("Error processing command", e);
            out.println("ERR REASON=" + e.getMessage());
        }
    }

    private void handleCreate() {
        String gameId = GameManager.getInstance().createGame();
        out.println("OK GAME_ID=" + gameId);
    }

    private void handleJoin(String[] parts) {
        try {
            String gameId = parseParam(parts, "GAME");
            String name = parseParam(parts, "NAME");

            GameManager.getInstance().findGame(gameId).ifPresentOrElse(foundTable -> {
                this.table = foundTable;
                this.player = new Player(java.util.UUID.randomUUID().toString(), name, 1000);

                table.addObserver(this);
                table.addPlayer(player);

                out.println("WELCOME PLAYER_ID=" + player.getId());

            }, () -> out.println("ERR REASON=GAME_NOT_FOUND"));

        } catch (Exception e) {
            out.println("ERR REASON=INVALID_PARAMS");
        }
    }

    private void handleStart() {
        if (table != null) {
            table.startGame();
        }
    }

    private void handleFold() {
        if (table != null && player != null) {
            table.playerFold(player);
            out.println("OK");
        }
    }

    private void handleCheck() {
        if (table != null && player != null) {
            table.playerCheck(player);
            out.println("OK");
        }
    }

    private void handleCall() {
        if (table != null && player != null) {
            table.playerCall(player);
            out.println("OK");
        }
    }

    private void handleRaise(String[] parts) {
        int amount = Integer.parseInt(parseParam(parts, "AMOUNT"));
        if (table != null && player != null) {
            table.playerRaise(player, amount);
            out.println("OK");
        }
    }

    private void handleDisconnect() {
        try {
            if (socketChannel != null && socketChannel.isOpen()) {
                socketChannel.close();
            }
        } catch (IOException e) {
            log.warn("Error closing socket during disconnect: {}", e.getMessage());
        }

        if (table != null && player != null) {
            try {
                table.playerDisconnect(player);
            } catch (Exception e) {
                log.error("Error handling player disconnect logic", e);
            }
        }

        log.info("Client handler resources released for player: {}",
                (player != null ? player.getName() : "Unknown"));
    }


    private void handleDraw(String[] parts) {
        // DRAW CARDS=0,2,4
        try {
            String cardsParam = parseParam(parts, "CARDS");
            List<Integer> indexes = new ArrayList<>();

            if (!cardsParam.equalsIgnoreCase("NONE")) {
                String[] split = cardsParam.split(",");
                for (String s : split) {
                    indexes.add(Integer.parseInt(s.trim()));
                }
            }

            if (table != null && player != null) {
                table.playerExchangeCards(player, indexes);
                out.println("OK");
            }
        } catch (NumberFormatException e) {
            out.println("ERR REASON=INVALID_CARD_INDEXES");
        } catch (Exception e) {
            out.println("ERR REASON=" + e.getMessage());
        }
    }


    private String parseParam(String[] parts, String key) {
        String prefix = key + "=";
        for (String part : parts) {
            if (part.startsWith(prefix)) {
                return part.substring(prefix.length());
            }
        }
        throw new IllegalArgumentException("Missing parameter: " + key);
    }
}

