package com.dnikitin.poker.game;

import com.dnikitin.poker.common.model.events.GameEvent;
import com.dnikitin.poker.common.model.events.GameObserver;
import com.dnikitin.poker.common.model.game.Card;
import com.dnikitin.poker.exceptions.moves.*;
import com.dnikitin.poker.exceptions.rules.IllegalPlayerAmountException;
import com.dnikitin.poker.game.engine.Dealer;
import com.dnikitin.poker.game.engine.PotManager;
import com.dnikitin.poker.game.engine.Round;
import com.dnikitin.poker.game.engine.TurnOrder;
import com.dnikitin.poker.game.setup.GameConfig;
import com.dnikitin.poker.game.setup.GameFactory;
import com.dnikitin.poker.game.state.GameState;
import com.dnikitin.poker.gamelogic.HandEvaluator;
import com.dnikitin.poker.model.Deck;
import com.dnikitin.poker.model.HandResult;
import com.dnikitin.poker.model.Player;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The central engine of a single Poker game instance.
 * <p>
 * <b>Architecture (Facade Pattern):</b>
 * This class acts as a Facade that coordinates lower-level components:
 * <ul>
 * <li>{@link com.dnikitin.poker.game.engine.Dealer} - Card distribution.</li>
 * <li>{@link com.dnikitin.poker.game.engine.PotManager} - Chip management.</li>
 * <li>{@link com.dnikitin.poker.game.engine.TurnOrder} - Player sequencing.</li>
 * <li>{@link com.dnikitin.poker.gamelogic.HandEvaluator} - Rule enforcement.</li>
 * </ul>
 * </p>
 * <p>
 * <b>Concurrency (Thread Safety):</b>
 * Since multiple com.dnikitin.poker.server.ClientHandler threads interact with the table simultaneously,
 * all state-mutating methods are protected by a {@link ReentrantLock}. This ensures atomic state transitions
 * and prevents race conditions (e.g., two players acting out of turn simultaneously).
 * </p>
 * <p>
 * <b>State Machine:</b>
 * The table transitions through strictly defined phases ({@link GameState}), ensuring valid game flow
 * (e.g., preventing betting during the dealing phase).
 * </p>
 */
@Slf4j
@Getter
public class Table {

    private final String id;
    private final GameFactory gameFactory;

    private final GameConfig config;
    private final HandEvaluator evaluator;

    // Helper components dealing with specific sub-domains
    private final Dealer dealer;
    private final PotManager potManager;
    private final TurnOrder turnOrder;
    private Round currentRound;

    private final List<Player> players = new ArrayList<>();
    private final List<GameObserver> observers = new ArrayList<>();

    /**
     * Explicit lock used to synchronize access to the game state.
     * Preferred over 'synchronized' blocks for better control and potential fairness policies.
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Current state of the game loop (e.g. LOBBY, BETTING, FINISHED).
     */
    private GameState currentState;

    private int drawsCompletedCount = 0;

    /**
     * Creates a new Table with specific rules provided by the factory.
     *
     * @param gameFactory Factory providing deck, evaluator, and config (Dependency Injection).
     */
    public Table(GameFactory gameFactory) {
        this.gameFactory = gameFactory;

        this.id = UUID.randomUUID().toString();
        this.config = gameFactory.createGameConfig();
        this.evaluator = gameFactory.createHandEvaluator();

        // Initialization of helper classes
        this.dealer = new Dealer(gameFactory.createDeck());
        this.potManager = new PotManager();

        // List 'players' is empty here, but TurnOrder holds the reference to it
        this.turnOrder = new TurnOrder(players);
        this.currentState = GameState.LOBBY;
        this.currentRound = new Round(GameState.LOBBY);
    }

    /**
     * Registers a new observer to receive game events.
     * Thread-safe.
     *
     * @param observer The listener (typically a client handler).
     */
    public void addObserver(GameObserver observer) {
        lock.lock();
        try {
            observers.add(observer);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds a player to the table.
     * Allowed only if the game is in LOBBY state and table is not full.
     */
    public void addPlayer(Player player) {
        lock.lock();
        try {
            if (currentState != GameState.LOBBY) {
                throw new WrongGameStateException("Game already started");
            }
            if (players.size() >= config.maxPlayers()) {
                throw new IllegalPlayerAmountException("Table is full");
            }
            players.add(player);
            log.info("Player {} joined table {}. Total players: {}", player.getName(), id, players.size());
            notifyObservers(new GameEvent.PlayerJoined(player.getId(), player.getName(), player.getChips()));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Starts the game loop. Validates minimum player count.
     */
    public void startGame() {
        lock.lock();
        try {
            if (players.size() < config.minPlayers()) {
                throw new IllegalPlayerAmountException("Not enough players to start. Min: " + config.minPlayers());
            }
            log.info("Starting game on table {}", id);
            notifyObservers(new GameEvent.GameStarted(id));
            startNewHand();
        } finally {
            lock.unlock();
        }
    }

    public Player getCurrentPlayer() {
        return turnOrder.getCurrentPlayer();
    }

    /**
     * Processes a player's request to CHECK.
     * <p>
     * Validation:
     * 1. Is it the betting phase?
     * 2. Is it this player's turn?
     * 3. Does the current bet equal the player's bet? (If not, Check is illegal).
     * </p>
     *
     * @param player The acting player.
     * @throws InvalidMoveException if the move violates poker rules.
     */
    public void playerCheck(Player player) {
        lock.lock();
        try {
            validateBettingPhase();
            validateTurn(player);
            if (player.getCurrentBet() < currentRound.getCurrentBet()) {
                throw new InvalidMoveException("Cannot CHECK, you must CALL " +
                        (currentRound.getCurrentBet() - player.getCurrentBet()));
            }

            currentRound.recordAction();
            log.info("Player {} checked.", player.getName());
            notifyObservers(new GameEvent.PlayerAction(player.getId(), "CHECK", 0, "Checked"));

            advanceTurn();
        } finally {
            lock.unlock();
        }
    }

    public void playerCall(Player player) {
        lock.lock();
        try {
            validateBettingPhase();
            validateTurn(player);
            int toCall = currentRound.getCurrentBet() - player.getCurrentBet();

            if (toCall > player.getChips()) {
                toCall = player.getChips();
            }

            if (toCall <= 0) {
                playerCheck(player); // If nothing to call, it's a check
                return;
            }

            player.bet(toCall);
            currentRound.recordAction();

            log.info("Player {} called {}.", player.getName(), toCall);
            notifyObservers(new GameEvent.PlayerAction(player.getId(), "CALL", toCall, "Called " + toCall));

            advanceTurn();
        } finally {
            lock.unlock();
        }
    }

    public void playerRaise(Player player, int raiseAmount) {
        lock.lock();
        try {
            validateBettingPhase();
            validateTurn(player);

            int minRaise = config.ante();
            if (raiseAmount < minRaise) {
                throw new InvalidMoveException("Raise amount too small. Minimum raise is " + minRaise);
            }

            int toCall = currentRound.getCurrentBet() - player.getCurrentBet();
            int totalAmount = toCall + raiseAmount;

            player.bet(totalAmount);

            // Update Round state (aggression resets action count in Round)
            currentRound.recordRaise(player.getCurrentBet(), turnOrder.getCurrentPlayerIndex());

            log.info("Player {} raised by {}. New highest bet: {}", player.getName(), raiseAmount, player.getCurrentBet());
            notifyObservers(new GameEvent.PlayerAction(
                    player.getId(), "RAISE", totalAmount, "Raised by " + raiseAmount));

            advanceTurn();
        } finally {
            lock.unlock();
        }
    }

    public void playerFold(Player player) {
        lock.lock();
        try {
            validateBettingPhase();
            validateTurn(player);
            player.fold();

            log.info("Player {} folded.", player.getName());
            notifyObservers(new GameEvent.PlayerAction(player.getId(), "FOLD", 0, "Folded"));

            if (turnOrder.countActivePlayers() < 2) {
                endGamePrematurely();
            } else {
                advanceTurn();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Processes a player's request to EXCHANGE cards (Draw phase).
     *
     * @param player           The acting player.
     * @param indexesToDiscard Indices of cards to remove from hand.
     * @throws IllegalDrawException if the deck runs out of cards.
     */
    public void playerExchangeCards(Player player, List<Integer> indexesToDiscard) {
        lock.lock();
        try {
            validateTurn(player);
            if (currentState != GameState.DRAWING) {
                throw new StateMismatchException("Cannot draw cards now. Current state: " + currentState);
            }

            if (indexesToDiscard.size() > config.maxDrawCount()) {
                throw new InvalidMoveException("Cannot discard more than " + config.maxDrawCount() + " cards.");
            }

            // SAFETY CHECK: Ensure we have enough cards in deck
            if (!dealer.hasEnoughCards(indexesToDiscard.size())) {
                throw new IllegalDrawException("Not enough cards in deck to exchange.");
            }

            // Delegate to Dealer
            List<Card> newCards = dealer.exchangeCards(player, indexesToDiscard);

            notifyObservers(new GameEvent.CardsDealt(player.getId(), player.getHand()));
            notifyObservers(new GameEvent.PlayerAction(player.getId(), "DRAW", 0, "Exchanged " + indexesToDiscard.size() + " cards"));

            drawsCompletedCount++;

            // Check if everyone has drawn
            if (drawsCompletedCount >= turnOrder.countActivePlayers()) {
                advanceGamePhase();
            } else {
                advanceTurn();
            }

        } finally {
            lock.unlock();
        }
    }

    public void playerDisconnect(Player player) {
        lock.lock();
        try {
            if (currentState == GameState.LOBBY) {
                players.remove(player);
                log.info("Player {} left table {} (Lobby). Total players: {}", player.getName(), id, players.size());
                notifyObservers(new GameEvent.PlayerAction(
                        player.getId(), "LEAVE", 0, "Disconnected"));
            } else if (currentState != GameState.FINISHED) {
                log.info("Player {} disconnected during game. Auto-folding.", player.getName());
                if (player.canAct()) {
                    player.fold();
                    notifyObservers(new GameEvent.PlayerAction(
                            player.getId(), "FOLD", 0, "Disconnected (Auto-Fold)"));
                    if (turnOrder.isCurrentPlayerIndex(players.indexOf(player))) {
                        advanceTurn();
                    }
                    if (turnOrder.countActivePlayers() < 2) {
                        endGamePrematurely();
                    }
                }
                player.setSittingOut();
            }
        } finally {
            lock.unlock();
        }
    }

    // --- Private / Internal Logic ---

    private void advanceTurn() {
        int activePlayers = turnOrder.countActivePlayers();

        // Check if betting round is complete using Round helper
        if (currentRound.isComplete(activePlayers)) {
            advanceGamePhase();
            return;
        }

        // Try to move to next player
        boolean success = turnOrder.nextPlayer();
        if (!success) {
            log.error("Critical error: No active players found.");
            endGamePrematurely();
            return;
        }

        Player current = getCurrentPlayer();

        int amountToCall = currentRound.getCurrentBet() - current.getCurrentBet();
        if (amountToCall < 0) amountToCall = 0;

        // to simplify = minRaise = ante
        int minRaise = config.ante();

        log.debug("Turn passed to: {}. To call: {}", current.getName(), amountToCall);

        notifyObservers(new GameEvent.TurnChanged(current.getId(), currentState.name(), amountToCall, minRaise));
    }

    private void advanceGamePhase() {
        collectBetsIntoPot();

        switch (currentState) {
            case BETTING_1, DRAWING -> {
                GameState nextState = (currentState == GameState.BETTING_1) ? GameState.DRAWING : GameState.BETTING_2;
                changeState(nextState);
                turnOrder.startFromLeftOfDealer();
                Player currentPlayer = getCurrentPlayer();


                int amountToCall = currentRound.getCurrentBet() - currentPlayer.getCurrentBet();
                if (amountToCall < 0) amountToCall = 0;

                int minRaise = config.ante();

                notifyObservers(new GameEvent.TurnChanged(
                        currentPlayer.getId(),
                        currentState.name(),
                        amountToCall,
                        minRaise
                ));
            }
            case BETTING_2 -> resolveShowdown();
            default -> log.error("Unexpected state transition from {}", currentState);
        }

        currentRound = new Round(currentState);
    }

    private void collectBetsIntoPot() {
        potManager.distributeBets(players);
        players.forEach(Player::resetRoundBet);
        // Use new event to inform clients about pot update
        notifyObservers(new GameEvent.RoundInfo(potManager.getTotalPot(), 0));
    }

    private void resolveShowdown() {
        changeState(GameState.SHOWDOWN);
        collectBetsIntoPot(); // Ensure final bets are in

        // 1. Reveal cards for all active players (including All-In)
        players.stream()
                .filter(Player::isActive) // REFACTORED: Use isActive() to include All-In players
                .forEach(p -> notifyObservers(new GameEvent.CardsDealt(p.getId(), p.getHand())));

        // 2. Iterate through all pots (Main + Side Pots)
        for (int i = 0; i < potManager.getPotCount(); i++) {
            PotManager.Pot pot = potManager.getPots().get(i);
            List<String> eligibleIds = pot.getEligiblePlayerIds();

            if (eligibleIds.isEmpty()) continue;

            // Find eligible players for this specific pot
            List<Player> eligiblePlayers = players.stream()
                    .filter(p -> eligibleIds.contains(p.getId()))
                    .filter(Player::isActive) // REFACTORED: Use isActive()
                    .toList();

            if (eligiblePlayers.isEmpty()) continue;

            List<Player> winners = findWinners(eligiblePlayers);
            for (Player p : eligiblePlayers) {
                HandResult result = evaluator.evaluate(p.getHand());
                String cards = result.getMainCards().toString();
                log.info("SHOWDOWN: Player {} has {}{}", p.getName(), result.getHandRank().getLabel(), cards);
            }

            if (winners.size() == 1) {
                Player winner = winners.getFirst();
                int amount = potManager.awardPot(i, winner);
                HandResult result = evaluator.evaluate(winner.getHand());
                notifyObservers(new GameEvent.GameFinished(
                        winner.getId(), amount, result.getHandRank().getLabel(), winner.getHand()));
            } else {
                Map<String, Integer> winnings = potManager.splitPot(i, winners);
                for (Player w : winners) {
                    HandResult result = evaluator.evaluate(w.getHand());

                    int amountWon = winnings.getOrDefault(w.getId(), 0);

                    notifyObservers(new GameEvent.GameFinished(
                            w.getId(), amountWon, "Split Pot: " + result.getHandRank().getLabel(), w.getHand()));
                }
            }
        }

        changeState(GameState.FINISHED);
    }

    private List<Player> findWinners(List<Player> candidates) {
        if (candidates.isEmpty()) return List.of();

        List<Player> winners = new ArrayList<>();
        HandResult bestHand = null;

        for (Player p : candidates) {
            HandResult result = evaluator.evaluate(p.getHand());
            if (bestHand == null || result.compareTo(bestHand) > 0) {
                bestHand = result;
                winners.clear();
                winners.add(p);
            } else if (result.compareTo(bestHand) == 0) {
                winners.add(p);
            }
        }
        return winners;
    }

    private void endGamePrematurely() {
        collectBetsIntoPot();

        // Find the last standing player (including All-In players who haven't folded)
        Player winner = players.stream()
                .filter(Player::isActive) // REFACTORED: Use isActive()
                .findFirst()
                .orElseThrow();

        int total = potManager.getTotalPot();
        winner.winChips(total);
        var rank = evaluator.evaluate(winner.getHand()).getHandRank().getLabel();


        log.info("Game ended prematurely. Player {} won {} (Opponents folded).", winner.getName(), total);

        notifyObservers(new GameEvent.GameFinished(winner.getId(), total, rank, winner.getHand()));
        changeState(GameState.FINISHED);
    }

    private void startNewHand() {
        turnOrder.rotateDealer();
        int minRaise = config.ante();

        potManager.reset();
        Deck newDeck = gameFactory.createDeck(); // Fabryka (produkcyjna lub testowa) tworzy deck
        dealer.setupNewDeck(newDeck);
        log.info("Hand started. Dealer: {}", turnOrder.getDealer().getName());

        players.forEach(Player::clearHand);
        players.forEach(Player::resetRoundBet);
        drawsCompletedCount = 0;

        changeState(GameState.ANTE);
        collectAnte();

        changeState(GameState.DEALING);
        dealer.dealInitialHands(players, 5);
        players.forEach(p -> notifyObservers(new GameEvent.CardsDealt(p.getId(), p.getHand())));

        turnOrder.startFromLeftOfDealer();

        changeState(GameState.BETTING_1);
        notifyObservers(new GameEvent.TurnChanged(getCurrentPlayer().getId(), currentState.name(),0, minRaise));
    }

    private void collectAnte() {
        int ante = config.ante();
        List<Player> bankrupts = new ArrayList<>();

        for (Player player : players) {
            try {
                player.bet(ante);
                potManager.addToMainPot(ante);
                player.resetRoundBet(); //to avoid double ante sum
                notifyObservers(new GameEvent.PlayerAction(player.getId(), "ANTE", ante, "Paid Ante"));
            } catch (NotEnoughChipsException e) {
                bankrupts.add(player);
                notifyObservers(new GameEvent.PlayerAction(player.getId(), "LEAVE", 0, "Bankrupt (Ante)"));
            }
        }
        players.removeAll(bankrupts);
        if (players.size() < 2) {
            throw new IllegalPlayerAmountException("Not enough players after Ante collection");
        }
        notifyObservers(new GameEvent.RoundInfo(potManager.getTotalPot(), 0));
    }

    private void validateTurn(Player player) {
        if (!player.getId().equals(getCurrentPlayer().getId())) {
            throw new OutOfTurnException();
        }
    }

    private void changeState(GameState newState) {
        log.info("State transition: {} -> {}", currentState, newState);
        currentState = newState;
        currentRound = new Round(newState);
        notifyObservers(new GameEvent.StateChanged(currentState.name()));
    }

    private void validateBettingPhase() {
        if (currentState != GameState.BETTING_1 && currentState != GameState.BETTING_2) {
            throw new InvalidMoveException("Action allowed only during betting phases. Current: " + currentState);
        }
    }

    private void notifyObservers(GameEvent event) {
        for (GameObserver observer : observers) {
            observer.onGameEvent(event);
        }
    }

    // Getters
    public int getPot() {
        return potManager.getTotalPot();
    }

    public int getCurrentRoundHighestBet() {
        return currentRound.getCurrentBet();
    }
}