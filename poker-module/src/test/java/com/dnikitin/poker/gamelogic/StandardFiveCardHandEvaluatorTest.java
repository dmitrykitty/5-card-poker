package com.dnikitin.poker.gamelogic;

import com.dnikitin.poker.common.model.game.Card;
import com.dnikitin.poker.common.model.game.HandRank;
import com.dnikitin.poker.common.model.game.Rank;
import com.dnikitin.poker.common.model.game.Suit;
import com.dnikitin.poker.exceptions.NoFiveCardsException;
import com.dnikitin.poker.exceptions.WrongRankException;
import com.dnikitin.poker.model.HandResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

public class StandardFiveCardHandEvaluatorTest {

    private final HandEvaluator evaluator = new StandardFiveCardHandEvaluator();

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideCards")
    void testCorrectHandEvaluating(String testName, HandRank expectedRank,
                                   List<Card> cardsFromPlayer,
                                   List<Card> expectedMainCards,
                                   List<Card> expectedOtherCards
    ) {
        HandResult result = evaluator.evaluate(cardsFromPlayer);
        assertAll(
                () -> assertThat(result.getHandRank()).isEqualTo(expectedRank),
                () -> assertThat(result.getMainCards()).containsExactlyInAnyOrderElementsOf(expectedMainCards),
                () -> assertThat(result.getOtherCards()).containsExactlyInAnyOrderElementsOf(expectedOtherCards)
        );
    }

    @Test
    void testThrowingExceptionWhenSixCardGiven() {
        List<Card> wrongCardsFromPlayer = List.of(
                new Card(Rank.ACE, Suit.DIAMONDS),
                new Card(Rank.ACE, Suit.DIAMONDS),
                new Card(Rank.ACE, Suit.DIAMONDS),
                new Card(Rank.ACE, Suit.DIAMONDS),
                new Card(Rank.ACE, Suit.DIAMONDS),
                new Card(Rank.ACE, Suit.DIAMONDS)
        );
        assertThatThrownBy(() -> evaluator.evaluate(wrongCardsFromPlayer))
                .isInstanceOf(NoFiveCardsException.class)
                .hasMessage("Required 5 cards in the end of game");
    }

    @Test
    void testThrowingExceptionForImpossibleState() {
        List<Card> impossibleHand = List.of(
                new Card(Rank.ACE, Suit.SPADES),
                new Card(Rank.ACE, Suit.HEARTS),
                new Card(Rank.ACE, Suit.DIAMONDS),
                new Card(Rank.ACE, Suit.CLUBS),
                new Card(Rank.ACE, Suit.SPADES)
        );

        assertThatThrownBy(() -> evaluator.evaluate(impossibleHand))
                .isInstanceOf(WrongRankException.class)
                .hasMessageContaining("Impossible state for 5 cards");
    }

    private static Stream<Arguments> provideCards() {
        return Stream.of(
                // ROYAL FLUSH
                Arguments.of("Royal Flush", HandRank.ROYAL_FLUSH,
                        List.of(
                                new Card(Rank.ACE, Suit.DIAMONDS),
                                new Card(Rank.KING, Suit.DIAMONDS),
                                new Card(Rank.QUEEN, Suit.DIAMONDS),
                                new Card(Rank.JACK, Suit.DIAMONDS),
                                new Card(Rank.TEN, Suit.DIAMONDS)
                        ),
                        List.of(
                                new Card(Rank.ACE, Suit.DIAMONDS),
                                new Card(Rank.KING, Suit.DIAMONDS),
                                new Card(Rank.QUEEN, Suit.DIAMONDS),
                                new Card(Rank.JACK, Suit.DIAMONDS),
                                new Card(Rank.TEN, Suit.DIAMONDS)
                        ),
                        List.of()
                ),
                //STRAIGHT FLUSH
                Arguments.of("Straight Flush", HandRank.STRAIGHT_FLUSH,
                        List.of(
                                new Card(Rank.JACK, Suit.CLUBS),
                                new Card(Rank.TEN, Suit.CLUBS),
                                new Card(Rank.NINE, Suit.CLUBS),
                                new Card(Rank.EIGHT, Suit.CLUBS),
                                new Card(Rank.SEVEN, Suit.CLUBS)
                        ),
                        List.of(
                                new Card(Rank.JACK, Suit.CLUBS),
                                new Card(Rank.TEN, Suit.CLUBS),
                                new Card(Rank.NINE, Suit.CLUBS),
                                new Card(Rank.EIGHT, Suit.CLUBS),
                                new Card(Rank.SEVEN, Suit.CLUBS)
                        ),
                        List.of()
                ),

                //FOUR OF A KIND
                Arguments.of("Four of a Kind", HandRank.FOUR_OF_A_KIND,
                        List.of(
                                new Card(Rank.EIGHT, Suit.CLUBS),
                                new Card(Rank.EIGHT, Suit.HEARTS),
                                new Card(Rank.EIGHT, Suit.DIAMONDS),
                                new Card(Rank.EIGHT, Suit.SPADES),
                                new Card(Rank.KING, Suit.CLUBS)
                        ),
                        List.of(
                                new Card(Rank.EIGHT, Suit.CLUBS),
                                new Card(Rank.EIGHT, Suit.HEARTS),
                                new Card(Rank.EIGHT, Suit.DIAMONDS),
                                new Card(Rank.EIGHT, Suit.SPADES)
                        ),
                        List.of(new Card(Rank.KING, Suit.CLUBS))
                ),

                //FULL HOUSE
                Arguments.of("Full House", HandRank.FULL_HOUSE,
                        List.of(
                                new Card(Rank.TEN, Suit.SPADES),
                                new Card(Rank.TEN, Suit.HEARTS),
                                new Card(Rank.TEN, Suit.CLUBS),
                                new Card(Rank.TWO, Suit.DIAMONDS),
                                new Card(Rank.TWO, Suit.SPADES)
                        ),
                        List.of(
                                new Card(Rank.TEN, Suit.SPADES),
                                new Card(Rank.TEN, Suit.HEARTS),
                                new Card(Rank.TEN, Suit.CLUBS),
                                new Card(Rank.TWO, Suit.DIAMONDS),
                                new Card(Rank.TWO, Suit.SPADES)
                        ),
                        List.of()
                ),

                //FLUSH
                Arguments.of("Flush", HandRank.FLUSH,
                        List.of(
                                new Card(Rank.ACE, Suit.DIAMONDS),
                                new Card(Rank.JACK, Suit.DIAMONDS),
                                new Card(Rank.NINE, Suit.DIAMONDS),
                                new Card(Rank.FOUR, Suit.DIAMONDS),
                                new Card(Rank.TWO, Suit.DIAMONDS)
                        ),
                        List.of(
                                new Card(Rank.ACE, Suit.DIAMONDS),
                                new Card(Rank.JACK, Suit.DIAMONDS),
                                new Card(Rank.NINE, Suit.DIAMONDS),
                                new Card(Rank.FOUR, Suit.DIAMONDS),
                                new Card(Rank.TWO, Suit.DIAMONDS)
                        ),
                        List.of()
                ),

                //STRAIGHT
                Arguments.of("Straight", HandRank.STRAIGHT,
                        List.of(
                                new Card(Rank.SIX, Suit.SPADES),
                                new Card(Rank.FIVE, Suit.HEARTS),
                                new Card(Rank.FOUR, Suit.DIAMONDS),
                                new Card(Rank.THREE, Suit.CLUBS),
                                new Card(Rank.TWO, Suit.SPADES)
                        ),
                        List.of(
                                new Card(Rank.SIX, Suit.SPADES),
                                new Card(Rank.FIVE, Suit.HEARTS),
                                new Card(Rank.FOUR, Suit.DIAMONDS),
                                new Card(Rank.THREE, Suit.CLUBS),
                                new Card(Rank.TWO, Suit.SPADES)
                        ),
                        List.of()
                ),

                //STEEL WHEEL STRAIGHT
                Arguments.of("Steel Wheel Straight (A-5)", HandRank.STRAIGHT,
                        List.of(
                                new Card(Rank.ACE, Suit.SPADES),
                                new Card(Rank.TWO, Suit.HEARTS),
                                new Card(Rank.THREE, Suit.CLUBS),
                                new Card(Rank.FOUR, Suit.DIAMONDS),
                                new Card(Rank.FIVE, Suit.SPADES)
                        ),
                        List.of(
                                new Card(Rank.ACE, Suit.SPADES),
                                new Card(Rank.FIVE, Suit.SPADES),
                                new Card(Rank.FOUR, Suit.DIAMONDS),
                                new Card(Rank.THREE, Suit.CLUBS),
                                new Card(Rank.TWO, Suit.HEARTS)
                        ),
                        List.of()
                ),

                //THREE OF A KIND
                Arguments.of("Three of a Kind", HandRank.THREE_OF_A_KIND,
                        List.of(
                                new Card(Rank.SEVEN, Suit.SPADES),
                                new Card(Rank.SEVEN, Suit.HEARTS),
                                new Card(Rank.SEVEN, Suit.CLUBS),
                                new Card(Rank.KING, Suit.DIAMONDS),
                                new Card(Rank.TWO, Suit.SPADES)
                        ),
                        List.of(
                                new Card(Rank.SEVEN, Suit.SPADES),
                                new Card(Rank.SEVEN, Suit.HEARTS),
                                new Card(Rank.SEVEN, Suit.CLUBS)
                        ),
                        List.of(
                                new Card(Rank.KING, Suit.DIAMONDS),
                                new Card(Rank.TWO, Suit.SPADES)
                        )
                ),

                //TWO PAIRS
                Arguments.of("Two Pairs", HandRank.TWO_PAIRS,
                        List.of(
                                new Card(Rank.JACK, Suit.SPADES),
                                new Card(Rank.JACK, Suit.HEARTS),
                                new Card(Rank.TEN, Suit.CLUBS),
                                new Card(Rank.TEN, Suit.DIAMONDS),
                                new Card(Rank.ACE, Suit.SPADES)
                        ),
                        List.of(
                                new Card(Rank.JACK, Suit.SPADES),
                                new Card(Rank.JACK, Suit.HEARTS),
                                new Card(Rank.TEN, Suit.CLUBS),
                                new Card(Rank.TEN, Suit.DIAMONDS)
                        ),
                        List.of(new Card(Rank.ACE, Suit.SPADES))
                ),

                //ONE PAIR
                Arguments.of("One Pair", HandRank.ONE_PAIR,
                        List.of(
                                new Card(Rank.TWO, Suit.SPADES),
                                new Card(Rank.TWO, Suit.HEARTS),
                                new Card(Rank.KING, Suit.CLUBS),
                                new Card(Rank.QUEEN, Suit.DIAMONDS),
                                new Card(Rank.JACK, Suit.SPADES)
                        ),
                        List.of(
                                new Card(Rank.TWO, Suit.SPADES),
                                new Card(Rank.TWO, Suit.HEARTS)
                        ),
                        List.of(
                                new Card(Rank.KING, Suit.CLUBS),
                                new Card(Rank.QUEEN, Suit.DIAMONDS),
                                new Card(Rank.JACK, Suit.SPADES)
                        )
                ),

                //HIGH CARD
                Arguments.of("High Card", HandRank.HIGH_CARD,
                        List.of(
                                new Card(Rank.ACE, Suit.SPADES),
                                new Card(Rank.JACK, Suit.HEARTS),
                                new Card(Rank.NINE, Suit.CLUBS),
                                new Card(Rank.SEVEN, Suit.DIAMONDS),
                                new Card(Rank.FIVE, Suit.SPADES)
                        ),
                        List.of(new Card(Rank.ACE, Suit.SPADES)),
                        List.of(
                                new Card(Rank.JACK, Suit.HEARTS),
                                new Card(Rank.NINE, Suit.CLUBS),
                                new Card(Rank.SEVEN, Suit.DIAMONDS),
                                new Card(Rank.FIVE, Suit.SPADES)
                        )
                )
        );
    }
}
