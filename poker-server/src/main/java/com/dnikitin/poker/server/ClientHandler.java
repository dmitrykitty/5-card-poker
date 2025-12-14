package com.dnikitin.poker.server;

import com.dnikitin.poker.common.exceptions.ProtocolException;
import com.dnikitin.poker.common.exceptions.SecurityException;
import com.dnikitin.poker.common.model.events.GameEvent;
import com.dnikitin.poker.common.model.events.GameObserver;
import com.dnikitin.poker.common.protocol.Command;
import com.dnikitin.poker.common.protocol.ProtocolEncoder;
import com.dnikitin.poker.common.protocol.ProtocolParser;
import com.dnikitin.poker.common.protocol.commands.*;
import com.dnikitin.poker.exceptions.moves.InvalidMoveException;
import com.dnikitin.poker.game.GameManager;
import com.dnikitin.poker.game.Player;
import com.dnikitin.poker.game.Table;
import com.dnikitin.poker.server.security.ConnectionValidator;
import com.dnikitin.poker.server.security.RateLimiter;
import com.dnikitin.poker.server.security.TimeoutManager;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Handles a single client connection with protocol parsing and security.
 */
@Slf4j
public class ClientHandler implements Runnable, GameObserver {

    private final SocketChannel socketChannel;
    private final ProtocolParser parser;
    private final ProtocolEncoder encoder;
    private final ConnectionValidator validator;
    private final RateLimiter rateLimiter;
    private final TimeoutManager timeoutManager;
    private final String clientId;

    private PrintWriter out;
    private Player player;
    private Table table;

    public ClientHandler(SocketChannel socketChannel, RateLimiter rateLimiter, TimeoutManager timeoutManager) {
        this.socketChannel = socketChannel;
        this.parser = new ProtocolParser();
        this.encoder = new ProtocolEncoder();
        this.validator = new ConnectionValidator();
        this.rateLimiter = rateLimiter;
        this.timeoutManager = timeoutManager;
        this.clientId = UUID.randomUUID().toString();
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(Channels.newReader(socketChannel, StandardCharsets.UTF_8));
             PrintWriter printWriter = new PrintWriter(Channels.newWriter(socketChannel, StandardCharsets.UTF_8), true)) {

            out = printWriter;
            sendMessage(encoder.encodeHello("1.0"));
            log.info("Client {} connected", clientId);

            String line;
            while ((line = in.readLine()) != null) {
                try {
                    // Rate limiting check
                    if (!rateLimiter.allowMessage(clientId)) {
                        sendError("RATE_LIMIT", "Too many messages, slow down");
                        continue;
                    }

                    // Validate message security
                    validator.validateMessage(line);

                    // Parse and handle command
                    handleCommand(line.trim());

                } catch (SecurityException e) {
                    log.warn("Security violation from client {}: {}", clientId, e.getMessage());
                    sendError(e.getCode(), e.getMessage());
                    break; // Disconnect on security violation

                } catch (ProtocolException e) {
                    log.warn("Protocol error from client {}: {}", clientId, e.getMessage());
                    sendError(e.getCode(), e.getMessage());

                } catch (InvalidMoveException e) {
                    log.debug("Invalid move from client {}: {}", clientId, e.getMessage());
                    sendError("INVALID_MOVE", e.getMessage());

                } catch (Exception e) {
                    log.error("Unexpected error from client {}", clientId, e);
                    sendError("INTERNAL_ERROR", "An error occurred");
                }
            }

        } catch (IOException e) {
            log.info("Client {} disconnected: {}", clientId, e.getMessage());
        } finally {
            handleDisconnect();
        }
    }

    @Override
    public void onGameEvent(GameEvent event) {
        String message = encoder.encode(event);
        if (message != null) {
            // Special handling for CardsDealt - mask other players' cards
            if (event instanceof GameEvent.CardsDealt cd) {
                boolean isMyCards = player != null && cd.playerId().equals(player.getId());
                message = encoder.encodeCardsDealt(cd.playerId(), cd.cards(), !isMyCards);
            }

            // Special handling for TurnChanged - start timeout if it's our turn
            if (event instanceof GameEvent.TurnChanged tc) {
                if (player != null && tc.activePlayerId().equals(player.getId())) {
                    startTimeout();
                    log.debug("Started timeout for player {}", player.getName());
                }
            }

            sendMessage(message);
        }
    }

    private void handleCommand(String line) throws IOException {
        log.debug("Client {}: {}", clientId, line);

        Command command = parser.parse(line);

        // Dispatch command to appropriate handler
        switch (command.getType()) {
            case HELLO -> handleHello((HelloCommand) command);
            case CREATE -> handleCreate((CreateCommand) command);
            case JOIN -> handleJoin((JoinCommand) command);
            case START -> handleStart((SimpleCommand) command);
            case CALL -> handleCall((SimpleCommand) command);
            case CHECK -> handleCheck((SimpleCommand) command);
            case FOLD -> handleFold((SimpleCommand) command);
            case RAISE -> handleRaise((BetCommand) command);
            case DRAW -> handleDraw((DrawCommand) command);
            case STATUS -> handleStatus((SimpleCommand) command);
            case QUIT -> throw new IOException("Client requested quit");
            default -> sendError("UNKNOWN_COMMAND", "Command not supported: " + command.getType());
        }
    }

    private void handleHello(HelloCommand command) {
        log.info("Client {} hello: version {}", clientId, command.getVersion());
        sendMessage(encoder.encodeOk("Connected to poker server v1.0"));
    }

    private void handleCreate(CreateCommand command) {
        // Create a new game with specified config
        String gameId = GameManager.getInstance().createGame();
        sendMessage(encoder.encodeOk("GAME_ID=" + gameId));
        log.info("Client {} created game {}", clientId, gameId);
    }

    private void handleJoin(JoinCommand command) {
        // Validate player name
        if (!validator.isValidPlayerName(command.getName())) {
            sendError("INVALID_NAME", "Player name must be 2-20 alphanumeric characters");
            return;
        }

        // Validate game ID
        if (!validator.isValidGameId(command.getGameId())) {
            sendError("INVALID_GAME_ID", "Invalid game ID format");
            return;
        }

        GameManager.getInstance().findGame(command.getGameId()).ifPresentOrElse(foundTable -> {
            try {
                this.table = foundTable;
                this.player = new Player(UUID.randomUUID().toString(), command.getName(), 1000);

                table.addObserver(this);
                table.addPlayer(player);

                sendMessage(encoder.encodeWelcome(command.getGameId(), player.getId()));
                log.info("Client {} joined game {} as {}", clientId, command.getGameId(), player.getName());

            } catch (Exception e) {
                log.error("Error joining game", e);
                sendError("JOIN_FAILED", e.getMessage());
            }
        }, () -> sendError("GAME_NOT_FOUND", "Game does not exist"));
    }

    private void handleStart(SimpleCommand command) {
        validatePlayerAndTable();

        // Verify player is authorized to start (could check if they're the host)
        table.startGame();
        sendMessage(encoder.encodeOk("Game started"));
        log.info("Client {} started game", clientId);
    }

    private void handleCall(SimpleCommand command) {
        validatePlayerAndTable();
        cancelTimeout();

        table.playerCall(player);
        sendMessage(encoder.encodeOk());
    }

    private void handleCheck(SimpleCommand command) {
        validatePlayerAndTable();
        cancelTimeout();

        table.playerCheck(player);
        sendMessage(encoder.encodeOk());
    }

    private void handleFold(SimpleCommand command) {
        validatePlayerAndTable();
        cancelTimeout();

        table.playerFold(player);
        sendMessage(encoder.encodeOk());
    }

    private void handleRaise(BetCommand command) {
        validatePlayerAndTable();
        cancelTimeout();

        if (command.getAmount() <= 0) {
            sendError("INVALID_AMOUNT", "Raise amount must be positive");
            return;
        }

        table.playerRaise(player, command.getAmount());
        sendMessage(encoder.encodeOk());
    }

    private void handleDraw(DrawCommand command) {
        validatePlayerAndTable();
        cancelTimeout();

        // Validate card indexes
        for (int index : command.getCardIndexes()) {
            if (index < 0 || index > 4) {
                sendError("INVALID_CARD_INDEX", "Card index must be 0-4");
                return;
            }
        }

        if (command.getCardIndexes().size() > 3) {
            sendError("TOO_MANY_CARDS", "Cannot draw more than 3 cards");
            return;
        }

        table.playerExchangeCards(player, command.getCardIndexes());
        sendMessage(encoder.encodeOk());
    }

    private void handleStatus(SimpleCommand command) {
        validatePlayerAndTable();

        // Send current game status
        sendMessage(encoder.encodeRound(table.getPot(), table.getCurrentRoundHighestBet()));

        if (table.getCurrentPlayer() != null) {
            sendMessage(encoder.encodeTurn(
                table.getCurrentPlayer().getId(),
                table.getCurrentState().name(),
                table.getCurrentRoundHighestBet() - player.getCurrentBet(),
                10 // min raise - should come from config
            ));
        }
    }

    private void handleDisconnect() {
        // Cancel any active timeout
        cancelTimeout();

        // Remove from rate limiter
        rateLimiter.removeClient(clientId);

        try {
            if (socketChannel != null && socketChannel.isOpen()) {
                socketChannel.close();
            }
        } catch (IOException e) {
            log.warn("Error closing socket: {}", e.getMessage());
        }

        if (table != null && player != null) {
            try {
                table.playerDisconnect(player);
                log.info("Player {} disconnected from game", player.getName());
            } catch (Exception e) {
                log.error("Error handling player disconnect", e);
            }
        }

        log.info("Client {} disconnected", clientId);
    }

    private void handleTimeout() {
        log.warn("Player {} timed out, auto-folding", player != null ? player.getName() : "unknown");
        if (table != null && player != null) {
            try {
                table.playerFold(player);
            } catch (Exception e) {
                log.error("Error auto-folding timed out player", e);
            }
        }
    }

    private void startTimeout() {
        if (player != null && timeoutManager != null) {
            timeoutManager.startTimeout(player.getId(), this::handleTimeout);
        }
    }

    private void cancelTimeout() {
        if (player != null && timeoutManager != null) {
            timeoutManager.cancelTimeout(player.getId());
        }
    }

    private void validatePlayerAndTable() {
        if (table == null || player == null) {
            throw new InvalidMoveException("Not in a game");
        }
    }

    private void sendMessage(String message) {
        if (out != null) {
            out.println(message);
            log.debug("To client {}: {}", clientId, message);
        }
    }

    private void sendError(String code, String reason) {
        sendMessage(encoder.encodeError(code, reason));
    }

    // Package-private for testing
    String getClientId() {
        return clientId;
    }

    Player getPlayer() {
        return player;
    }
}
