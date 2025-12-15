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
    private int currentBet;
    private final List<String> myHand = new ArrayList<>();

    private String gameId;
    private String playerId;

    private int currentPot = 0;
    private int amountToCall = 0; // Ile muszę dołożyć, żeby wejść
    private String currentPhase = "WAITING";
    private String lastMessage = ""; // Ostatni komunikat systemowy

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

    public void updateRoundInfo(int pot) {
        this.currentPot = pot;
    }

    public void updateTurn(String phase, int toCall) {
        this.currentPhase = phase;
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

    public void setLastMessage(String msg) {
        this.lastMessage = msg;
    }


    public void reset() {
        this.gameId = null;
        this.playerId = null;
        this.playerNames.clear();
        this.playerChips.clear();
        this.currentPot = 0;
        this.myHand.clear();
        this.lastMessage = "";
        this.currentPhase = "WAITING";
    }

    public String getPlayerName(String id) {
        if (id == null) return "?";
        if (id.equals(this.playerId)) return "You";
        return playerNames.getOrDefault(id, id); // Zwraca imię lub ID
    }

    // Helper: Pobiera moje imię
    public String getMyName() {
        return playerNames.getOrDefault(playerId, "Unknown");
    }

    public int getMyChips() {
        return playerChips.getOrDefault(playerId, 0);
    }
}
