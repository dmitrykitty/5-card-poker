package com.dnikitin.poker.server;

import com.dnikitin.poker.common.exceptions.PokerSecurityException;
import com.dnikitin.poker.common.exceptions.ProtocolException;
import com.dnikitin.poker.common.model.events.GameEvent;
import com.dnikitin.poker.common.model.events.GameObserver;
import com.dnikitin.poker.common.protocol.clientserver.Command;
import com.dnikitin.poker.common.protocol.clientserver.ProtocolEncoder;
import com.dnikitin.poker.common.protocol.clientserver.ProtocolParser;
import com.dnikitin.poker.common.protocol.clientserver.commands.*;
import com.dnikitin.poker.exceptions.PokerGameException;
import com.dnikitin.poker.game.GameManager;
import com.dnikitin.poker.model.Player;
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
 * Handles the interaction with a single connected client.
 * <p>
 * This class acts as a bridge between the Networking layer (TCP Sockets) and the Game Logic layer.
 * It implements two key interfaces:
 * <ul>
 * <li>{@link Runnable}: Executes the main loop for reading incoming commands from the client.</li>
 * <li>{@link GameObserver}: Receives asynchronous updates from the {@link Table} (e.g., moves made by opponents)
 * and pushes them to the client via the socket.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Protocol Handling:</b>
 * Uses {@link ProtocolParser} to interpret raw strings and {@link ProtocolEncoder} to format responses.
 * </p>
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

    /**
     * Constructs a new handler for a given socket connection.
     *
     * @param socketChannel  The connected client socket.
     * @param rateLimiter    Shared rate limiter instance.
     * @param timeoutManager Shared timeout manager instance.
     */
    public ClientHandler(SocketChannel socketChannel, RateLimiter rateLimiter, TimeoutManager timeoutManager) {
        this.socketChannel = socketChannel;
        this.parser = new ProtocolParser();
        this.encoder = new ProtocolEncoder();
        this.validator = new ConnectionValidator();
        this.rateLimiter = rateLimiter;
        this.timeoutManager = timeoutManager;
        this.clientId = UUID.randomUUID().toString();
    }

    /**
     * The main execution loop for the client thread.
     * <p>
     * 1. Initializes input/output streams.<br>
     * 2. Performs the protocol handshake (HELLO).<br>
     * 3. Enters a loop reading lines from the socket until disconnection.<br>
     * 4. Validates, parses, and executes commands.<br>
     * 5. Handles exceptions and ensures proper resource cleanup on exit.
     * </p>
     */
    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(Channels.newReader(socketChannel, StandardCharsets.UTF_8));
             PrintWriter printWriter = new PrintWriter(Channels.newWriter(socketChannel, StandardCharsets.UTF_8), true)) {

            out = printWriter;
            sendMessage(encoder.encodeHello("1.0"));
            log.info("Client {} connected", clientId);

            String line;
            // Blocking read - effectively managed by Virtual Threads
            while ((line = in.readLine()) != null) {
                try {
                    if (!rateLimiter.allowMessage(clientId)) {
                        sendError("RATE_LIMIT", "Too many messages, slow down");
                        continue;
                    }
                    validator.validateMessage(line);
                    handleCommand(line.trim());
                } catch (PokerSecurityException e) {
                    log.warn("Security violation from client {}: {}", clientId, e.getMessage());
                    sendError(e.getCode(), e.getMessage());
                    break; // Disconnect on security violation
                } catch (ProtocolException e) {
                    log.warn("Protocol error from client {}: {}", clientId, e.getMessage());
                    sendError(e.getCode(), e.getMessage());
                } catch (PokerGameException e) {
                    // Logic errors (e.g., betting more than you have) are not fatal connection errors
                    log.debug("Game error [{}]: {}", e.getCode(), e.getMessage());
                    sendError(e.getCode(), e.getMessage());
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

    /**
     * Callback method triggered by the Game Engine when a state change occurs.
     * <p>
     * This method translates internal {@link GameEvent} objects into protocol string messages
     * and sends them to the client. It also manages turn-based timeouts.
     * </p>
     *
     * @param event The event that occurred in the game (e.g., DEAL, TURN_CHANGED).
     */
    @Override
    public void onGameEvent(GameEvent event) {
        String message = encoder.encode(event);
        if (message != null) {
            // Special handling for CardsDealt to mask opponent cards
            if (event instanceof GameEvent.CardsDealt cd) {
                boolean isMyCards = player != null && cd.playerId().equals(player.getId());
                message = encoder.encodeCardsDealt(cd.playerId(), cd.cards(), !isMyCards);
            }
            // Detect if it is this player's turn to start the countdown timer
            if (event instanceof GameEvent.TurnChanged tc) {
                if (player != null && tc.activePlayerId().equals(player.getId())) {
                    startTimeout();
                    log.debug("Started timeout for player {}", player.getName());
                }
            }
            sendMessage(message);
        }
    }

    /**
     * Dispatches the parsed command to the appropriate handler method.
     */
    void handleCommand(String line) throws IOException {
        log.debug("Client {}: {}", clientId, line);
        Command command = parser.parse(line);
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
        String gameId = GameManager.getInstance().createGame();
        sendMessage(encoder.encodeOk("GAME_ID=" + gameId));
        log.info("Client {} created game {}", clientId, gameId);
    }

    private void handleJoin(JoinCommand command) {
        if (!validator.isValidPlayerName(command.getName())) {
            sendError("INVALID_NAME", "Player name must be 2-20 alphanumeric characters");
            return;
        }
        if (!validator.isValidGameId(command.getGameId())) {
            sendError("INVALID_GAME_ID", "Invalid game ID format");
            return;
        }

        GameManager.getInstance().findGame(command.getGameId()).ifPresentOrElse(foundTable -> {
            try {
                this.table = foundTable;
                this.player = new Player(UUID.randomUUID().toString(), command.getName(), 1000);

                // Subscribe to game events before adding the player
                table.addObserver(this);
                table.addPlayer(player);

                sendMessage(encoder.encodeWelcome(command.getGameId(), player.getId(), player.getName()));

                // Send list of existing players to the new player (Lobby Sync)
                for (Player existing : table.getPlayers()) {
                    if (!existing.getId().equals(player.getId())) {
                        String lobbyMsg = String.format("LOBBY PLAYER=%s CHIPS=%d NAME=%s",
                                existing.getId(), existing.getChips(), existing.getName());
                        sendMessage(lobbyMsg);
                    }
                }

                log.info("Client {} joined game {} as {}", clientId, command.getGameId(), player.getName());
            } catch (PokerGameException e) {
                log.warn("Game error during join from client {}: {}", clientId, e.getMessage());
                sendError(e.getCode(), e.getMessage());
            }
        }, () -> sendError("GAME_NOT_FOUND", "Game does not exist"));
    }

    // ... (rest of the command handlers: handleStart, handleCall, etc. follow standard logic)

    private void handleStart(SimpleCommand command) {
        validatePlayerAndTable();
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
        sendMessage(encoder.encodeRound(table.getPot(), table.getCurrentRoundHighestBet()));
        if (table.getCurrentPlayer() != null) {
            int minRaise = table.getConfig().ante();
            sendMessage(encoder.encodeTurn(
                    table.getCurrentPlayer().getId(),
                    table.getCurrentState().name(),
                    table.getCurrentRoundHighestBet() - player.getCurrentBet(),
                    minRaise
            ));
        }
    }

    /**
     * Cleans up resources when the client disconnects.
     * <p>
     * Removes the player from the game table (if joined), cancels any active timeouts,
     * and closes the socket connection.
     * </p>
     */
    private void handleDisconnect() {
        cancelTimeout();
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

    private void handleTimeout(String timeoutPlayerId) {
        if (player == null || !player.getId().equals(timeoutPlayerId)) {
            return;
        }
        log.warn("Player {} timed out, auto-folding", player.getName());
        if (table != null) {
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
            throw new com.dnikitin.poker.exceptions.moves.InvalidMoveException("Not in a game");
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

    String getClientId() {
        return clientId;
    }

    Player getPlayer() {
        return player;
    }
}