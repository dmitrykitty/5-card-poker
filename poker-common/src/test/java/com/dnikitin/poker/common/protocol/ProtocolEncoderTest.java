package com.dnikitin.poker.common.protocol;

import com.dnikitin.poker.common.model.events.GameEvent;
import com.dnikitin.poker.common.model.game.Card;
import com.dnikitin.poker.common.model.game.Rank;
import com.dnikitin.poker.common.model.game.Suit;
import com.dnikitin.poker.common.protocol.clientserver.ProtocolEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolEncoderTest {

    private ProtocolEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new ProtocolEncoder();
    }

    @Test
    void testEncodeOk() {
        String result = encoder.encodeOk();
        assertEquals("OK", result);
    }

    @Test
    void testEncodeOkWithMessage() {
        String result = encoder.encodeOk("Action completed");
        assertEquals("OK MESSAGE=Action completed", result);
    }

    @Test
    void testEncodeError() {
        String result = encoder.encodeError("INVALID_MOVE", "Not your turn");
        assertEquals("ERR CODE=INVALID_MOVE REASON=Not_your_turn", result);
    }

    @Test
    void testEncodeWelcome() {
        String result = encoder.encodeWelcome("game123", "player456");
        assertEquals("WELCOME GAME=game123 PLAYER=player456", result);
    }

    @Test
    void testEncodeHello() {
        String result = encoder.encodeHello("1.0");
        assertEquals("HELLO VERSION=1.0", result);
    }

    @Test
    void testEncodePlayerJoinedEvent() {
        GameEvent event = new GameEvent.PlayerJoined("p1", "Alice", 1000);
        String result = encoder.encode(event);

        assertNotNull(result);
        assertTrue(result.contains("LOBBY"));
        assertTrue(result.contains("PLAYER=p1"));
        assertTrue(result.contains("CHIPS=1000"));
        assertTrue(result.contains("NAME=Alice"));
    }

    @Test
    void testEncodeGameStartedEvent() {
        GameEvent event = new GameEvent.GameStarted("game789");
        String result = encoder.encode(event);

        assertNotNull(result);
        assertTrue(result.contains("STARTED"));
        assertTrue(result.contains("GAME=game789"));
    }

    @Test
    void testEncodeStateChangedEvent() {
        GameEvent event = new GameEvent.StateChanged("BETTING_1");
        String result = encoder.encode(event);

        assertNotNull(result);
        assertTrue(result.contains("STATE"));
        assertTrue(result.contains("PHASE=BETTING_1"));
    }

    @Test
    void testEncodeTurnChangedEvent() {
        GameEvent event = new GameEvent.TurnChanged("player2");
        String result = encoder.encode(event);

        assertNotNull(result);
        assertTrue(result.contains("TURN"));
        assertTrue(result.contains("PLAYER=player2"));
    }

    @Test
    void testEncodePlayerActionEvent() {
        GameEvent event = new GameEvent.PlayerAction("p1", "BET", 50, "Bet 50");
        String result = encoder.encode(event);

        assertNotNull(result);
        assertTrue(result.contains("ACTION"));
        assertTrue(result.contains("PLAYER=p1"));
        assertTrue(result.contains("TYPE=BET"));
        assertTrue(result.contains("AMOUNT=50"));
        assertTrue(result.contains("MSG=Bet 50"));
    }

    @Test
    void testEncodeCardsDealtEvent() {
        List<Card> cards = Arrays.asList(
            new Card(Rank.ACE, Suit.SPADES),
            new Card(Rank.KING, Suit.HEARTS),
            new Card(Rank.TEN, Suit.DIAMONDS)
        );
        GameEvent event = new GameEvent.CardsDealt("p1", cards);
        String result = encoder.encode(event);

        assertNotNull(result);
        assertTrue(result.contains("DEAL"));
        assertTrue(result.contains("PLAYER=p1"));
        assertTrue(result.contains("CARDS="));
    }

    @Test
    void testEncodeGameFinishedEvent() {
        GameEvent event = new GameEvent.GameFinished("p1", 500, "Pair of Aces");
        String result = encoder.encode(event);

        assertNotNull(result);
        assertTrue(result.contains("WINNER"));
        assertTrue(result.contains("PLAYER=p1"));
        assertTrue(result.contains("POT=500"));
        assertTrue(result.contains("RANK=Pair of Aces"));
    }

    @Test
    void testEncodeCardsDealtWithMask() {
        List<Card> cards = Arrays.asList(
            new Card(Rank.ACE, Suit.SPADES),
            new Card(Rank.KING, Suit.HEARTS)
        );
        String result = encoder.encodeCardsDealt("p1", cards, true);

        assertTrue(result.contains("HIDDEN"));
        assertTrue(result.contains("COUNT=2"));
    }

    @Test
    void testEncodeCardsDealtWithoutMask() {
        List<Card> cards = Arrays.asList(
            new Card(Rank.ACE, Suit.SPADES),
            new Card(Rank.KING, Suit.HEARTS)
        );
        String result = encoder.encodeCardsDealt("p1", cards, false);

        assertTrue(result.contains("DEAL"));
        assertTrue(result.contains("PLAYER=p1"));
        assertTrue(result.contains("CARDS="));
        assertFalse(result.contains("HIDDEN"));
    }

    @Test
    void testEncodeTurnWithBettingInfo() {
        String result = encoder.encodeTurn("p1", "BET1", 50, 20);

        assertTrue(result.contains("TURN"));
        assertTrue(result.contains("PLAYER=p1"));
        assertTrue(result.contains("PHASE=BET1"));
        assertTrue(result.contains("CALL=50"));
        assertTrue(result.contains("MINRAISE=20"));
    }

    @Test
    void testEncodeRound() {
        String result = encoder.encodeRound(300, 100);

        assertTrue(result.contains("ROUND"));
        assertTrue(result.contains("POT=300"));
        assertTrue(result.contains("HIGHESTBET=100"));
    }

    @Test
    void testEncodeShowdown() {
        List<Card> cards = Arrays.asList(
            new Card(Rank.ACE, Suit.SPADES),
            new Card(Rank.ACE, Suit.HEARTS)
        );
        String result = encoder.encodeShowdown("p1", cards, "Pair");

        assertTrue(result.contains("SHOWDOWN"));
        assertTrue(result.contains("PLAYER=p1"));
        assertTrue(result.contains("RANK=Pair"));
    }

    @Test
    void testEncodePayout() {
        String result = encoder.encodePayout("p1", 500, 1200);

        assertTrue(result.contains("PAYOUT"));
        assertTrue(result.contains("PLAYER=p1"));
        assertTrue(result.contains("AMOUNT=500"));
        assertTrue(result.contains("STACK=1200"));
    }

    @Test
    void testSanitizeRemovesSpecialCharacters() {
        String result = encoder.encodeError("TEST", "Error with spaces and @#$");
        assertTrue(result.contains("REASON=Error_with_spaces_and_"));
        assertFalse(result.contains("@"));
        assertFalse(result.contains("#"));
    }
}
