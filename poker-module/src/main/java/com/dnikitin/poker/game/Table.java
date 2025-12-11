package com.dnikitin.poker.game;

import com.dnikitin.poker.common.model.Card;
import com.dnikitin.poker.exceptions.IllegalPlayerAmountException;
import com.dnikitin.poker.exceptions.InvalidMoveException;
import com.dnikitin.poker.exceptions.NotEnoughChipsException;
import com.dnikitin.poker.exceptions.WrongGameStateException;
import com.dnikitin.poker.gamelogic.HandEvaluator;
import com.dnikitin.poker.model.Deck;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single Poker Table instance.
 * <p>
 * This class acts as the central Game Engine. It manages the lifecycle of the game
 * (State Machine), holds the deck and players, and enforces turn order.
 * It connects the "Physics" (Deck, Cards) with the "Rules" (Evaluator, Config).
 * </p>
 */
@Slf4j
@Getter
public class Table {

    private final String id;
    private final GameConfig config;
    private final HandEvaluator evaluator;

    private final List<Player> players = new ArrayList<>();
    private Deck deck;

    /**
     * Current state of the game loop (e.g. LOBBY, BETTING, FINISHED).
     */
    private GameState currentState;

    /**
     * Total amount of chips currently in the pot for this hand.
     */
    private int pot;

    /**
     * Index of the player who holds the "Dealer Button".
     * This player acts last in betting rounds. Rotates every hand.
     */
    private int dealerIndex = -1;

    /**
     * Index of the player expected to make a move right now.
     */
    private int currentPlayerIndex = 0;

    /**
     * Creates a new Table with specific rules provided by the factory.
     *
     * @param gameFactory Factory providing deck, evaluator, and config.
     */
    public Table(GameFactory gameFactory) {
        id = UUID.randomUUID().toString();
        config = gameFactory.createGameConfig();
        evaluator = gameFactory.createHandEvaluator();
        deck = gameFactory.createDeck();
        currentState = GameState.LOBBY;
    }

    /**
     * Adds a player to the table.
     * Allowed only if the game is in LOBBY state and table is not full.
     *
     * @param player The player to join.
     * @throws WrongGameStateException      if game has already started.
     * @throws IllegalPlayerAmountException if table is full.
     */
    public void addPlayer(Player player) {
        if (currentState != GameState.LOBBY) {
            throw new WrongGameStateException("Game already started");
        }
        if (players.size() >= config.maxPlayers()) {
            throw new IllegalPlayerAmountException("Table is full");
        }
        players.add(player);
        log.info("Player {} joined table {}. Total players: {}", player.getName(), id, players.size());
    }

    /**
     * Starts the game loop. Validates minimum player count.
     * Triggers the first hand.
     *
     * @throws IllegalPlayerAmountException if there are not enough players.
     */
    public void startGame() {
        if (players.size() < config.minPlayers()) {
            throw new IllegalPlayerAmountException("Not enough players to start. Min: " + config.minPlayers());
        }
        log.info("Starting game on table {}", id);
        startNewHand();
    }

    /**
     * Returns the Player object who is currently expected to act.
     * Used by the Server to validate incoming commands.
     *
     * @return The active Player or null if list is empty.
     */
    public Player getCurrentPlayer() {
        return players.isEmpty() ? null : players.get(currentPlayerIndex);
    }

    /**
     * Advances the turn to the next active (not folded) player.
     * Wraps around the list size.
     */
    public void nextTurn() {
        if (!players.isEmpty()) {
            int attempts = 0;
            do {
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
                attempts++;
            } while (getCurrentPlayer().isFolded() && attempts < players.size());
        }

        log.debug("Turn passed to: {}", getCurrentPlayer().getName());
    }

    /**
     * Handles a CHECK or CALL action from a player.
     * <p>
     * Validates turn order and advances the game state.
     * In the simplified version, this just passes the turn.
     * </p>
     *
     * @param player The player performing the action.
     * @throws InvalidMoveException if it is not the player's turn.
     */
    public void playerCheckOrCall(Player player){
        validateTurn(player);

        nextTurn();


    }

    /**
     * Handles a FOLD action from a player.
     * <p>
     * Marks the player as folded for the current hand and passes the turn.
     * </p>
     *
     * @param player The player performing the action.
     * @throws InvalidMoveException if it is not the player's turn.
     */
    public void playerFold(Player player){
        validateTurn(player);

        player.fold();
        log.info("Player {} folded.", player.getName());
        nextTurn();
    }
    //PRIVATE HELPERS

    /**
     * Orchestrates the beginning of a new hand (round).
     * Resets state, shuffles deck, collects ante, and deals cards.
     */
    private void startNewHand() {

        rotateDealer();

        pot = 0;
        deck = Deck.createDeck(); //each game - new Deck
        deck.shuffle();

        players.forEach(Player::clearHand);
        players.forEach(Player::resetRoundBet);

        changeState(GameState.ANTE);
        collectAnte();

        changeState(GameState.DEALING);
        dealCards();

        currentPlayerIndex = (dealerIndex + 1) % players.size(); //left of the dealer

        changeState(GameState.BETTING_1);
        log.info("""
                        Hand started. Dealer: {}.
                        First to act: {}.
                        Pot: {}.
                        Deck size remaining: {}
                        """,
                players.get(dealerIndex).getName(),
                getCurrentPlayer().getName(),
                pot,
                deck.size());
    }

    /**
     * Validates if the player making a request is the one currently allowed to move.
     *
     * @param player The player to validate.
     * @throws InvalidMoveException if IDs do not match.
     */
    private void validateTurn(Player player){
        if(!player.getId().equals(getCurrentPlayer().getId())){
            throw new InvalidMoveException("Not your turn!");
        }
    }

    /**
     * Moves the dealer button to the next player.
     * Should be called once per hand.
     */
    private void rotateDealer() {
        if (!players.isEmpty()) {
            if (dealerIndex == -1) {
                dealerIndex = 0; // First game
            } else {
                dealerIndex = (dealerIndex + 1) % players.size();
            }

            // Safety check if a player left
            if (dealerIndex >= players.size()) {
                dealerIndex = 0;
            }
        }
    }

    /**
     * Distributes 5 cards to each active player from the deck.
     */
    private void dealCards() {
        log.info("Dealing 5 cards to each active player.");
        for (Player player : players) {
            List<Card> hand = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                hand.add(deck.deal());
            }
            player.receiveCards(hand);
        }
    }

    /**
     * Collects mandatory ante chips from all players.
     * Removes players who cannot afford to pay.
     */
    private void collectAnte() {
        int ante = config.ante();
        log.info("Collecting ante: {}", ante);

        List<Player> bankrupts = new ArrayList<>();
        for (Player player : players) {
            try {
                player.bet(ante);
                pot += ante;
            } catch (NotEnoughChipsException e) {
                log.warn("Player {} cannot pay ante and has been removed. The reason: {}", player.getName(), e.getMessage());
                bankrupts.add(player);
            }
        }
        players.removeAll(bankrupts);
        if (players.size() < 2) {
            throw new IllegalPlayerAmountException("Not enough players after Ante collection");
        }
    }

    /**
     * Transitions the game state machine and logs the change.
     */
    private void changeState(GameState newState) {
        log.info("State transition: {} -> {}", currentState, newState);
        currentState = newState;
    }
}
