package com.dnikitin.poker.common.protocol.clientserver;

import com.dnikitin.poker.common.model.events.GameEvent;
import com.dnikitin.poker.common.model.game.Card;

import java.util.List;

/**
 * Transforms internal {@link GameEvent} objects into text-based protocol messages
 * intended for the client.
 * <p>
 * This class handles:
 * <ul>
 * <li><b>Serialization:</b> Formatting complex objects (like Lists of Cards) into string representations.</li>
 * <li><b>Sanitization:</b> Ensuring user-generated content (like messages) does not break the protocol format
 * (e.g., by replacing spaces with underscores).</li>
 * <li><b>Information Hiding:</b> Masking sensitive data, such as opponent cards.</li>
 * </ul>
 * </p>
 */
public class ProtocolEncoder {

    /**
     * Converts a GameEvent into its string protocol representation.
     * <p>
     * Uses Java 17+ pattern matching for switch to exhaustively handle all
     * {@link GameEvent} permits.
     * </p>
     *
     * @param event The internal game event.
     * @return The formatted protocol string ready to be sent over the socket.
     */
    public String encode(GameEvent event) {
        return switch (event) {
            case GameEvent.PlayerJoined pj -> String.format("LOBBY PLAYER=%s CHIPS=%d NAME=%s",
                    pj.playerId(), pj.chips(), pj.name());

            case GameEvent.GameStarted gs -> String.format("STARTED GAME=%s", gs.gameId());

            case GameEvent.StateChanged sc -> String.format("STATE PHASE=%s", sc.newState());

            case GameEvent.TurnChanged tc ->
                    encodeTurn(tc.activePlayerId(), tc.phase(), tc.amountToCall(), tc.minRaise());

            case GameEvent.PlayerAction pa -> String.format("ACTION PLAYER=%s TYPE=%s AMOUNT=%d MSG=%s",
                    pa.playerId(), pa.actionType(), pa.amount(), sanitize(pa.message()));

            case GameEvent.CardsDealt cd -> String.format("DEAL PLAYER=%s CARDS=%s",
                    cd.playerId(), formatCards(cd.cards()));

            case GameEvent.GameFinished gf -> String.format("WINNER PLAYER=%s POT=%d RANK=%s CARDS=%s",
                    gf.winnerId(), gf.potAmount(), sanitize(gf.handRank()), formatCards(gf.cards()));

            case GameEvent.RoundInfo ri -> encodeRound(ri.potAmount(), ri.highestBet());
        };
    }

    /**
     * Encodes a standard OK acknowledgement.
     */
    public String encodeOk() {
        return "OK";
    }

    /**
     * Encodes an OK acknowledgement with an informational message.
     */
    public String encodeOk(String message) {
        return String.format("OK MESSAGE=%s", sanitize(message));
    }

    /**
     * Encodes an error response.
     *
     * @param code   The error code (machine readable).
     * @param reason The error description (human readable).
     */
    public String encodeError(String code, String reason) {
        return String.format("ERR CODE=%s REASON=%s", code, sanitize(reason));
    }

    /**
     * Encodes a WELCOME message sent specifically to the player joining the game.
     */
    public String encodeWelcome(String gameId, String playerId, String playerName) {
        return String.format("WELCOME GAME=%s PLAYER=%s NAME=%s", gameId, playerId, sanitize(playerName));
    }

    /**
     * Encodes the initial handshake HELLO message.
     */
    public String encodeHello(String version) {
        return String.format("HELLO VERSION=%s", version);
    }

    /**
     * Encodes a DEAL event, optionally masking the cards.
     * <p>
     * <b>Security Critical:</b> This method must be called with {@code mask=true}
     * when sending dealing info to opponents, so they cannot see the player's private cards.
     * </p>
     *
     * @param playerId The player receiving cards.
     * @param cards    The list of cards.
     * @param mask     If true, card details are replaced with "HIDDEN" and only the count is sent.
     */
    public String encodeCardsDealt(String playerId, List<Card> cards, boolean mask) {
        if (mask) {
            return String.format("DEAL PLAYER=%s CARDS=HIDDEN COUNT=%d",
                    playerId, cards.size());
        }
        return String.format("DEAL PLAYER=%s CARDS=%s",
                playerId, formatCards(cards));
    }

    public String encodeTurn(String playerId, String phase, int callAmount, int minRaise) {
        return String.format("TURN PLAYER=%s PHASE=%s CALL=%d MINRAISE=%d",
                playerId, phase, callAmount, minRaise);
    }

    public String encodeRound(int pot, int highestBet) {
        return String.format("ROUND POT=%d HIGHESTBET=%d", pot, highestBet);
    }

    /**
     * Formats a list of Card objects into a comma-separated string (e.g., "Ah,Ks,2d").
     */
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

    /**
     * Sanitizes strings to prevent protocol injection.
     * Replaces spaces with underscores and removes forbidden characters.
     */
    private String sanitize(String text) {
        if (text == null) return "";
        // Replace spaces with underscores and remove special characters
        return text.replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9_=-]", "");
    }
}