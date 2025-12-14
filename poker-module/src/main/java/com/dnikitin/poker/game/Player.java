package com.dnikitin.poker.game;

import com.dnikitin.poker.common.model.game.Card;
import com.dnikitin.poker.exceptions.moves.NotEnoughChipsException;
import com.dnikitin.poker.game.state.PlayerStatus;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a participant in the poker game.
 * <p>
 * This class maintains the state of a single player, including their
 * hand of cards, chip balance, and current status (active, folded, betting).
 * It does not contain game rules logic but ensures the internal consistency
 * of the player's state (e.g., preventing betting more chips than owned).
 * </p>
 */
@Getter
@ToString(of = {"id", "name"}, includeFieldNames = false)
public class Player {

    private final String id;
    private final String name;

    private int chips;
    private int currentBet;
    /**
     * Flag indicating if the player has folded (resigned) in the current hand.
     */
    private boolean folded;
    private PlayerStatus status;
    private final List<Card> hand = new ArrayList<>();

    /**
     * Creates a new player with an initial balance.
     *
     * @param id            Unique identifier.
     * @param name          Display name.
     * @param startingChips Initial amount of chips.
     */
    public Player(String id, String name, int startingChips) {
        this.id = id;
        this.name = name;
        this.chips = startingChips;
        this.folded = false;
        this.status = PlayerStatus.ACTIVE;
    }

    /**
     * Adds a list of cards to the player's hand.
     * Used during the initial deal and after drawing cards.
     *
     * @param cards The list of cards to add.
     */
    public void receiveCards(List<Card> cards){
        hand.addAll(cards);
    }

    /**
     * Places a bet by moving chips from the balance to the current bet.
     *
     * @param betAmount The amount of chips to bet.
     * @throws NotEnoughChipsException if the player does not have enough chips.
     */
    public void bet(int betAmount){
        if(betAmount > chips)
            throw new NotEnoughChipsException("Not enough chips");
        chips -= betAmount;
        currentBet += betAmount;

        // Check if player is now all-in
        if (chips == 0 && status == PlayerStatus.ACTIVE) {
            status = PlayerStatus.ALL_IN;
        }
    }

    /**
     * Removes cards from the player's hand based on their indexes.
     * <p>
     * This method sorts the provided indexes in descending order before removal
     * to prevent index shifting issues (removing a lower index first would shift
     * subsequent elements, invalidating higher indexes).
     * </p>
     *
     * @param indexes A list of zero-based indexes of cards to discard.
     */
    public void discardCards(List<Integer> indexes){
        indexes.stream()
                .sorted(Collections.reverseOrder()) // Sort descending to avoid index shifting
                .forEach(i -> hand.remove((int) i));

    }

    /**
     * Adds chips to the player's balance.
     * Typically called when the player wins a pot.
     *
     * @param amount The amount of chips won.
     */
    public void winChips(int amount) {
        chips += amount;
    }

    /**
     * Marks the player as folded.
     * A folded player cannot participate in further betting or the showdown.
     */
    public void fold(){
        folded = true;
        status = PlayerStatus.FOLDED;
    }

    /**
     * Resets the betting state for a new betting round.
     * <p>
     * This sets {@code currentBet} to 0 but preserves the {@code folded} status
     * and the {@code hand}, as the game hand is not yet over.
     * </p>
     */
    public void resetRoundBet(){
        currentBet = 0;
    }

    /**
     * Clears the player's hand and resets status for a completely new game hand.
     * <p>
     * This removes all cards and sets {@code folded} to false.
     * Should be called when the game moves from FINISHED back to ANTE/DEALING.
     * </p>
     */
    public void clearHand(){
        hand.clear();
        folded = false;
        if (chips > 0) {
            status = PlayerStatus.ACTIVE;
        }
    }

    /**
     * Checks if the player is still active in the current hand.
     *
     * @return {@code true} if the player has not folded and has chips remaining;
     * {@code false} otherwise.
     */
    public boolean isActive() {
        return !folded && status != PlayerStatus.SITTING_OUT;
    }

    /**
     * Checks if the player is all-in.
     *
     * @return {@code true} if the player has no chips left but is still in the hand
     */
    public boolean isAllIn() {
        return status == PlayerStatus.ALL_IN || (chips == 0 && !folded);
    }

    /**
     * Sets the player status to sitting out.
     */
    public void setSittingOut() {
        status = PlayerStatus.SITTING_OUT;
    }

    /**
     * Checks if the player can act (not folded and not all-in).
     *
     * @return {@code true} if the player can make actions
     */
    public boolean canAct() {
        return status == PlayerStatus.ACTIVE && !folded;
    }
}
