package com.dnikitin.poker.common.protocol;

import com.dnikitin.poker.common.model.events.GameEvent;
import com.dnikitin.poker.common.model.game.Card;

import java.util.List;

/**
 * Encodes game events into protocol messages to send to clients.
 * Server-to-Client message format.
 */
public class ProtocolEncoder {

    /**
     * Converts a GameEvent into a protocol message string.
     *
     * @param event The game event to encode
     * @return Protocol message string
     */
    public String encode(GameEvent event) {
        return switch (event) {
            case GameEvent.PlayerJoined pj ->
                String.format("LOBBY PLAYER=%s CHIPS=%d NAME=%s",
                    pj.playerId(), pj.chips(), pj.name());

            case GameEvent.GameStarted gs ->
                String.format("STARTED GAME=%s", gs.gameId());

            case GameEvent.StateChanged sc ->
                String.format("STATE PHASE=%s", sc.newState());

            case GameEvent.TurnChanged tc ->
                String.format("TURN PLAYER=%s", tc.activePlayerId());

            case GameEvent.PlayerAction pa ->
                String.format("ACTION PLAYER=%s TYPE=%s AMOUNT=%d MSG=%s",
                    pa.playerId(), pa.actionType(), pa.amount(), pa.message());

            case GameEvent.CardsDealt cd ->
                String.format("DEAL PLAYER=%s CARDS=%s",
                    cd.playerId(), formatCards(cd.cards()));

            case GameEvent.GameFinished gf ->
                String.format("WINNER PLAYER=%s POT=%d RANK=%s",
                    gf.winnerId(), gf.potAmount(), gf.handRank());

            case GameEvent.RoundInfo ri ->
                    encodeRound(ri.potAmount(), ri.highestBet());

            default -> null;
        };
    }

    /**
     * Encodes an OK response.
     */
    public String encodeOk() {
        return "OK";
    }

    /**
     * Encodes an OK response with a message.
     */
    public String encodeOk(String message) {
        return String.format("OK MESSAGE=%s", message);
    }

    /**
     * Encodes an error response.
     */
    public String encodeError(String code, String reason) {
        return String.format("ERR CODE=%s REASON=%s", code, sanitize(reason));
    }

    /**
     * Encodes a WELCOME message after successful join.
     */
    public String encodeWelcome(String gameId, String playerId) {
        return String.format("WELCOME GAME=%s PLAYER=%s", gameId, playerId);
    }

    /**
     * Encodes a HELLO message from server.
     */
    public String encodeHello(String version) {
        return String.format("HELLO VERSION=%s", version);
    }

    /**
     * Encodes cards dealt to a specific player.
     * For other players, cards should be masked.
     */
    public String encodeCardsDealt(String playerId, List<Card> cards, boolean mask) {
        if (mask) {
            return String.format("DEAL PLAYER=%s CARDS=HIDDEN COUNT=%d",
                playerId, cards.size());
        }
        return String.format("DEAL PLAYER=%s CARDS=%s",
            playerId, formatCards(cards));
    }

    /**
     * Encodes a turn notification with betting information.
     */
    public String encodeTurn(String playerId, String phase, int callAmount, int minRaise) {
        return String.format("TURN PLAYER=%s PHASE=%s CALL=%d MINRAISE=%d",
            playerId, phase, callAmount, minRaise);
    }

    /**
     * Encodes round information (pot and highest bet).
     */
    public String encodeRound(int pot, int highestBet) {
        return String.format("ROUND POT=%d HIGHESTBET=%d", pot, highestBet);
    }

    /**
     * Encodes showdown information.
     */
    public String encodeShowdown(String playerId, List<Card> hand, String rank) {
        return String.format("SHOWDOWN PLAYER=%s HAND=%s RANK=%s",
            playerId, formatCards(hand), rank);
    }

    /**
     * Encodes payout information.
     */
    public String encodePayout(String playerId, int amount, int stackLeft) {
        return String.format("PAYOUT PLAYER=%s AMOUNT=%d STACK=%d",
            playerId, amount, stackLeft);
    }

    private String formatCards(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return "NONE";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(cards.get(i).toString());
        }
        return sb.toString();
    }

    private String sanitize(String text) {
        if (text == null) return "";
        // Replace spaces with underscores and remove special characters
        return text.replaceAll("\\s+", "_")
                   .replaceAll("[^a-zA-Z0-9_-]", "");
    }
}
