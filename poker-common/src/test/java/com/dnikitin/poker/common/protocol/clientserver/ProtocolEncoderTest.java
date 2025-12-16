package com.dnikitin.poker.common.protocol.clientserver;

import com.dnikitin.poker.common.model.events.GameEvent;
import com.dnikitin.poker.common.model.game.Card;
import com.dnikitin.poker.common.model.game.Rank;
import com.dnikitin.poker.common.model.game.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolEncoderTest {

    private ProtocolEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new ProtocolEncoder();
    }

    @Test
    @DisplayName("Should encode simple OK messages")
    void testEncodeOk() {
        assertThat(encoder.encodeOk()).isEqualTo("OK");
        assertThat(encoder.encodeOk("Everything good"))
                .isEqualTo("OK MESSAGE=Everything_good");
    }

    @Test
    @DisplayName("Should encode ERROR messages with sanitization")
    void testEncodeError() {
        String result = encoder.encodeError("BAD_REQ", "Invalid input!");
        assertThat(result).isEqualTo("ERR CODE=BAD_REQ REASON=Invalid_input");
    }

    @Test
    @DisplayName("Should encode PlayerJoined event")
    void testEncodePlayerJoined() {
        GameEvent event = new GameEvent.PlayerJoined("p-123", "Alice", 1000);
        String result = encoder.encode(event);

        assertThat(result).isEqualTo("LOBBY PLAYER=p-123 CHIPS=1000 NAME=Alice");
    }

    @Test
    @DisplayName("Should encode GameStarted event")
    void testEncodeGameStarted() {
        GameEvent event = new GameEvent.GameStarted("game-uuid");
        String result = encoder.encode(event);

        assertThat(result).isEqualTo("STARTED GAME=game-uuid");
    }

    @Test
    @DisplayName("Should encode CardsDealt event (Open cards)")
    void testEncodeCardsDealt() {
        List<Card> cards = List.of(
                new Card(Rank.ACE, Suit.SPADES),
                new Card(Rank.TEN, Suit.HEARTS)
        );
        GameEvent event = new GameEvent.CardsDealt("p-1", cards);
        String result = encoder.encode(event);

        assertThat(result).startsWith("DEAL PLAYER=p-1 CARDS=");
        assertThat(result).contains(cards.get(0).toString());
        assertThat(result).contains(cards.get(1).toString());
    }

    @Test
    @DisplayName("Should encode CardsDealt with masking (Hidden cards)")
    void testEncodeCardsDealtMasked() {
        List<Card> cards = List.of(
                new Card(Rank.ACE, Suit.SPADES),
                new Card(Rank.TEN, Suit.HEARTS)
        );
        String result = encoder.encodeCardsDealt("p-2", cards, true);

        assertThat(result).isEqualTo("DEAL PLAYER=p-2 CARDS=HIDDEN COUNT=2");
    }

    @Test
    @DisplayName("Should encode PlayerAction event")
    void testEncodePlayerAction() {
        GameEvent event = new GameEvent.PlayerAction("p-1", "RAISE", 50, "Raised by 50");
        String result = encoder.encode(event);

        assertThat(result).isEqualTo("ACTION PLAYER=p-1 TYPE=RAISE AMOUNT=50 MSG=Raised_by_50");
    }

    @Test
    @DisplayName("Should encode TurnChanged event")
    void testEncodeTurnChanged() {
        GameEvent event = new GameEvent.TurnChanged("p-next", "BETTING_1", 100, 20);
        String result = encoder.encode(event);

        assertThat(result).isEqualTo("TURN PLAYER=p-next PHASE=BETTING_1 CALL=100 MINRAISE=20");
    }

    @Test
    @DisplayName("Should encode GameFinished event with winning cards")
    void testEncodeGameFinished() {
        List<Card> winningHand = List.of(
                new Card(Rank.ACE, Suit.SPADES),
                new Card(Rank.KING, Suit.HEARTS)
        );

        GameEvent event = new GameEvent.GameFinished("p-winner", 500, "Full_House", winningHand);

        String result = encoder.encode(event);

        String expectedCardsStr = winningHand.get(0).toString() + "," + winningHand.get(1).toString();

        assertThat(result).isEqualTo("WINNER PLAYER=p-winner POT=500 RANK=Full_House CARDS=" + expectedCardsStr);
    }

    @Test
    @DisplayName("Should encode RoundInfo event")
    void testEncodeRoundInfo() {
        GameEvent event = new GameEvent.RoundInfo(250, 50);
        String result = encoder.encode(event);

        assertThat(result).isEqualTo("ROUND POT=250 HIGHESTBET=50");
    }
}
