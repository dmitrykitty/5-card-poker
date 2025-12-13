package com.dnikitin.poker.model;

import com.dnikitin.poker.common.exceptions.EmptyDeckException;
import com.dnikitin.poker.common.model.game.Card;
import com.dnikitin.poker.common.model.game.Rank;
import com.dnikitin.poker.common.model.game.Suit;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.random.RandomGenerator;

@Slf4j
@ToString(onlyExplicitlyIncluded = true)
public class Deck {

    private final LinkedList<Card> cards;
    private final RandomGenerator random; //use of Secure Random

    private Deck(List<Card> cards, RandomGenerator random) {
        this.cards = new LinkedList<>(cards);
        this.random = random;
    }

    public static Deck createDeck() {
        List<Card> cards = new LinkedList<>();

        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                Card card = new Card(rank, suit);
                cards.add(card);
            }
        }
        return new Deck(cards, new SecureRandom());
    }

    public void shuffle() {
        Collections.shuffle(cards, (SecureRandom) random);
        log.info("Deck shuffled. Cards count: {}", cards.size());

    }

    public Card deal() {
        if (cards.isEmpty()) {
            throw new EmptyDeckException("Cannot deal from an empty deck");
        }
        return cards.removeFirst();
    }

    @ToString.Include
    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
