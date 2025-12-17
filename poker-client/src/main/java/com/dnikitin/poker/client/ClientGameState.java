package com.dnikitin.poker.client;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the mutable state of the client-side game.
 * <p>
 * This class serves as a data holder synchronized between the network thread
 * (which updates the state based on server messages) and the UI thread
 * (which renders this state to the console).
 * </p>
 * <p>
 * Thread Safety: Uses {@link ConcurrentHashMap} for player data to ensure safe concurrent access.
 * primitive fields are not volatile, assuming updates happen mostly sequentially from the server listener.
 * </p>
 */
@Getter
public class ClientGameState {

    /**
     * Maps player IDs to their display names.
     */
    private final Map<String, String> playerNames = new ConcurrentHashMap<>();

    /**
     * Maps player IDs to their current chip count.
     */
    private final Map<String, Integer> playerChips = new ConcurrentHashMap<>();

    // Game state fields

    /**
     * The highest bet placed in the current round.
     */
    private int currentBet;

    /**
     * Total number of chips currently in the pot.
     */
    private int currentPot = 0;

    /**
     * The amount of chips the player needs to add to match the current bet.
     */
    private int amountToCall = 0;

    /**
     * The player's current hand of cards (e.g., "Ah", "10s").
     */
    private final List<String> myHand = new ArrayList<>();

    /**
     * Unique identifier for the current game session.
     */
    @Setter
    private String gameId;

    /**
     * Unique identifier for the current player provided by the server.
     */
    @Setter
    private String playerId;

    /**
     * Current phase of the game (e.g., "BETTING", "DRAW", "WAITING").
     */
    private String currentPhase = "WAITING";

    /**
     * The last system message or notification received from the server.
     */
    @Setter
    private String lastMessage = "";

    /**
     * Updates or adds a player's information in the local state.
     *
     * @param id    The unique player ID.
     * @param name  The display name of the player (can be null if not updating).
     * @param chips The chip count (if negative, the value is ignored).
     */
    public void updatePlayerInfo(String id, String name, int chips) {
        if (name != null) playerNames.put(id, name);
        if (chips >= 0) playerChips.put(id, chips);
    }

    /**
     * Deducts a specific amount of chips from a player.
     *
     * @param id     The player ID.
     * @param amount The amount to subtract.
     */
    public void deductChips(String id, int amount) {
        playerChips.computeIfPresent(id, (key, current) -> current - amount);
    }

    /**
     * Adds a specific amount of chips to a player.
     *
     * @param id     The player ID.
     * @param amount The amount to add.
     */
    public void addChips(String id, int amount) {
        playerChips.computeIfPresent(id, (key, current) -> current + amount);
    }

    /**
     * Sets the session identifiers upon joining a game.
     *
     * @param gameId   The game session ID.
     * @param playerId The player's assigned ID.
     */
    public void setConnectionInfo(String gameId, String playerId) {
        this.gameId = gameId;
        this.playerId = playerId;
    }

    /**
     * Updates the global round information such as pot size and highest bet.
     *
     * @param pot        The current total pot.
     * @param highestBet The current highest bet on the table.
     */
    public void updateRoundInfo(int pot, int highestBet) {
        this.currentPot = pot;
        this.currentBet = highestBet;
    }

    /**
     * Updates the current game phase.
     *
     * @param phase The new phase name.
     */
    public void updatePhase(String phase) {
        this.currentPhase = phase;
    }

    /**
     * Updates the betting requirements for the current player's turn.
     *
     * @param toCall The amount required to call the current bet.
     */
    public void updateTurnInfo(int toCall) {
        this.amountToCall = toCall;
    }

    /**
     * Parses and updates the player's hand from a string representation.
     *
     * @param cardsString A comma-separated string of cards (e.g., "Ah,Ks,2d") or special tokens like "NONE"/"HIDDEN".
     */
    public void updateMyHand(String cardsString) {
        myHand.clear();
        if (cardsString != null && !cardsString.equals("NONE") && !cardsString.equals("HIDDEN")) {
            String[] cards = cardsString.split(",");
            for (String card : cards) {
                myHand.add(card.trim());
            }
        }
    }

    /**
     * Resets the entire game state to default values.
     * Usually called when leaving a game or when a critical error occurs.
     */
    public void reset() {
        this.gameId = null;
        this.playerId = null;
        this.playerNames.clear();
        this.playerChips.clear();
        this.currentPot = 0;
        this.currentBet = 0;
        this.amountToCall = 0;
        this.myHand.clear();
        this.lastMessage = "";
        this.currentPhase = "WAITING";
    }

    /**
     * Resolves a player ID to a display name.
     *
     * @param id The player ID.
     * @return "You" if the ID matches the local player, the player's name if known, or the ID itself as a fallback.
     */
    public String getPlayerName(String id) {
        if (id == null) return "?";
        if (id.equals(this.playerId)) return "You";
        return playerNames.getOrDefault(id, id);
    }

    /**
     * Gets the local player's display name.
     *
     * @return The name or "Unknown" if not set.
     */
    public String getMyName() {
        return playerNames.getOrDefault(playerId, "Unknown");
    }

    /**
     * Gets the local player's current chip count.
     *
     * @return Chip count or 0 if unknown.
     */
    public int getMyChips() {
        return playerChips.getOrDefault(playerId, 0);
    }
}