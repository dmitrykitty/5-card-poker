package com.dnikitin.poker.model;

import com.dnikitin.poker.common.exceptions.EmptyDeckException;
import com.dnikitin.poker.common.model.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


public class DeckTest {

    Deck deck;

    @BeforeEach
    public void setUp() {
        deck = Deck.createDeck();
    }

    @Test
    public void is52UniqueCardsCreated() {
        Set<Card> cards = new HashSet<>();
        int size = deck.size();

        while (!deck.isEmpty()) {
            cards.add(deck.deal());
        }
        assertThat(size).isEqualTo(cards.size());
    }

    @Test
    public void isTwoDecksAreTheSameBeforeShuffle() {
        Deck deck2 = Deck.createDeck();

        while (!deck.isEmpty()) {
            Card card1 = deck.deal();
            Card card2 = deck2.deal();
            assertThat(card1).isEqualTo(card2);
        }

        assertAll(
                () -> assertThat(deck.size()).isZero(),
                () -> assertThat(deck2.size()).isZero()
        );
    }

    @Test
    public void isDealOnEmptyDeckZThrowingException() {
        while (!deck.isEmpty()) {
            deck.deal();
        }
        assertThrows(EmptyDeckException.class, () -> deck.deal());
    }

    @Test
    public void isShuffleMakingDecksUnique() {
        Deck deckShuffled1 = Deck.createDeck();
        Deck deckShuffled2 = Deck.createDeck();

        deckShuffled1.shuffle();
        deckShuffled2.shuffle();

        List<Card> sortedCards = extractAllCards(deck);
        List<Card> cards1 = extractAllCards(deckShuffled1);
        List<Card> cards2 = extractAllCards(deckShuffled2);

        assertAll(
                //is all elements equals
                () -> assertThat(cards1).containsExactlyInAnyOrderElementsOf(sortedCards),
                () -> assertThat(cards2).containsExactlyInAnyOrderElementsOf(sortedCards),

                //is order the same
                () -> assertThat(cards1).isNotEqualTo(sortedCards),
                () -> assertThat(cards1).isNotEqualTo(cards2)
        );
    }


    private List<Card> extractAllCards(Deck deck) {
        List<Card> cards = new ArrayList<>();
        while (!deck.isEmpty()) {
            cards.add(deck.deal());
        }
        return cards;
    }
}
