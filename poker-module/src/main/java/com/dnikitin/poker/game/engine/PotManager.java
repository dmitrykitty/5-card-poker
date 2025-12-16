package com.dnikitin.poker.game.engine;

import com.dnikitin.poker.model.Player;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the main pot and side pots for all-in scenarios.
 * Ensures pot calculations are correct and handles complex split scenarios.
 */
@Slf4j
@Getter
public class PotManager {

    /**
     * Represents a single pot (main or side pot).
     */
    @Getter
    public static class Pot {
        private int amount;
        private final List<String> eligiblePlayerIds;

        public Pot(List<String> eligiblePlayerIds) {
            this.amount = 0;
            this.eligiblePlayerIds = new ArrayList<>(eligiblePlayerIds);
        }

        public void addToPot(int chips) {
            amount += chips;
        }

        public void clear() {
            amount = 0;
        }

        @Override
        public String toString() {
            return String.format("Pot{amount=%d, eligible=%d players}", amount, eligiblePlayerIds.size());
        }
    }

    private final List<Pot> pots = new ArrayList<>();
    private Pot mainPot;

    public PotManager() {
        reset();
    }

    /**
     * Resets the pot manager for a new hand.
     */
    public void reset() {
        pots.clear();
        mainPot = new Pot(new ArrayList<>());
        pots.add(mainPot);
        log.debug("PotManager reset");
    }

    /**
     * Adds chips to the main pot.
     *
     * @param amount The amount to add
     */
    public void addToMainPot(int amount) {
        mainPot.addToPot(amount);
        log.debug("Added {} to main pot. Total: {}", amount, mainPot.getAmount());
    }

    /**
     * Gets the total amount across all pots.
     *
     * @return Total pot amount
     */
    public int getTotalPot() {
        return pots.stream().mapToInt(Pot::getAmount).sum();
    }

    /**
     * Distributes bets from all players, creating side pots if necessary.
     * This method handles all-in scenarios where players have different bet amounts.
     *
     * @param players List of all players in the hand
     */
    public void distributeBets(List<Player> players) {
        int currentMainPotAmount = mainPot.getAmount();
        pots.clear();

        // Group players by their current bet amount
        Map<Integer, List<Player>> betLevels = new HashMap<>();


        for (Player player : players) {
            if (player.getCurrentBet() > 0) {
                betLevels.computeIfAbsent(player.getCurrentBet(), k -> new ArrayList<>())
                    .add(player);
            }
        }

        // If all bets are equal, simple case - add to main pot
        if (betLevels.size() <= 1) {
            int totalBets = players.stream().mapToInt(Player::getCurrentBet).sum();

            List<String> activePlayerIds = players.stream()
                    .filter(p -> !p.isFolded())
                    .map(Player::getId)
                    .toList();

            mainPot = new Pot(activePlayerIds);
            mainPot.addToPot(currentMainPotAmount + totalBets);

            pots.add(mainPot);
            return;
        }

        // Complex case: create side pots for different bet levels
        List<Integer> sortedBetLevels = betLevels.keySet().stream()
            .sorted()
            .toList();

        int previousLevel = 0;
        for (int betLevel : sortedBetLevels) {
            int betForThisPot = betLevel - previousLevel;

            // Collect contributions for this pot level
            List<String> eligiblePlayers = new ArrayList<>();
            int potAmount = 0;

            for (Player player : players) {
                if (player.getCurrentBet() >= betLevel) {
                    potAmount += betForThisPot;
                    if (!player.isFolded()) {
                        eligiblePlayers.add(player.getId());
                    }
                }
            }

            if (potAmount > 0) {
                Pot pot = new Pot(eligiblePlayers);
                pot.addToPot(potAmount);
                pots.add(pot);
                log.debug("Created pot: {} with {} eligible players",
                    potAmount, eligiblePlayers.size());
            }

            previousLevel = betLevel;
        }
    }

    /**
     * Awards a pot to a winning player.
     *
     * @param potIndex The index of the pot to award
     * @param winner   The winning player
     * @return The amount won
     */
    public int awardPot(int potIndex, Player winner) {
        if (potIndex < 0 || potIndex >= pots.size()) {
            log.error("Invalid pot index: {}", potIndex);
            return 0;
        }

        Pot pot = pots.get(potIndex);
        if (!pot.getEligiblePlayerIds().contains(winner.getId())) {
            log.warn("Player {} not eligible for pot {}", winner.getId(), potIndex);
            return 0;
        }

        int amount = pot.getAmount();
        winner.winChips(amount);

        pot.clear();

        log.info("Awarded {} from pot {} to player {}", amount, potIndex, winner.getName());
        return amount;
    }

    /**
     * Splits a pot equally among multiple winners.
     *
     * @param potIndex The index of the pot to split
     * @param winners  List of winning players
     */
    public void splitPot(int potIndex, List<Player> winners) {
        if (potIndex < 0 || potIndex >= pots.size() || winners.isEmpty()) {
            return;
        }

        Pot pot = pots.get(potIndex);
        int share = pot.getAmount() / winners.size();
        int remainder = pot.getAmount() % winners.size();

        for (int i = 0; i < winners.size(); i++) {
            Player winner = winners.get(i);
            int amount = share + (i < remainder ? 1 : 0); // Distribute remainder
            winner.winChips(amount);
            log.info("Split pot: awarded {} to player {}", amount, winner.getName());
        }
        pot.clear();
    }

    /**
     * Gets the number of pots (main + side pots).
     *
     * @return Number of pots
     */
    public int getPotCount() {
        return pots.size();
    }
}
