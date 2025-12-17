package com.dnikitin.poker.client;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class ClientGameState {
    private final Map<String, String> playerNames = new ConcurrentHashMap<>();
    private final Map<String, Integer> playerChips = new ConcurrentHashMap<>();

    // Pola stanu gry
    private int currentBet;      // Najwyższy zakład na stole
    private int currentPot = 0;
    private int amountToCall = 0; // Ile muszę dołożyć w danej turze

    private final List<String> myHand = new ArrayList<>();

    @Setter
    private String gameId;
    @Setter
    private String playerId;

    private String currentPhase = "WAITING";
    @Setter
    private String lastMessage = "";

    public void updatePlayerInfo(String id, String name, int chips) {
        if (name != null) playerNames.put(id, name);
        if (chips >= 0) playerChips.put(id, chips);
    }

    public void deductChips(String id, int amount) {
        playerChips.computeIfPresent(id, (key, current) -> current - amount);
    }

    public void addChips(String id, int amount) {
        playerChips.computeIfPresent(id, (key, current) -> current + amount);
    }

    public void setConnectionInfo(String gameId, String playerId) {
        this.gameId = gameId;
        this.playerId = playerId;
    }

    public void updateRoundInfo(int pot, int highestBet) {
        this.currentPot = pot;
        this.currentBet = highestBet; // Wcześniej to było ignorowane!
    }

    public void updatePhase(String phase) {
        this.currentPhase = phase;
    }


    public void updateTurnInfo(int toCall) {
        this.amountToCall = toCall;
    }

    public void updateMyHand(String cardsString) {
        myHand.clear();
        if (cardsString != null && !cardsString.equals("NONE") && !cardsString.equals("HIDDEN")) {
            String[] cards = cardsString.split(",");
            for (String card : cards) {
                myHand.add(card.trim());
            }
        }
    }

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

    public String getPlayerName(String id) {
        if (id == null) return "?";
        if (id.equals(this.playerId)) return "You";
        return playerNames.getOrDefault(id, id);
    }

    public String getMyName() {
        return playerNames.getOrDefault(playerId, "Unknown");
    }

    public int getMyChips() {
        return playerChips.getOrDefault(playerId, 0);
    }
}