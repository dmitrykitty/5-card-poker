package com.dnikitin.poker.common.model.game;

import lombok.NonNull;

public record Card(Rank rank, Suit suit) implements Comparable<Card> {


    @Override

    public int compareTo(@NonNull Card other) {
        int compare = Integer.compare(rank.getPower(), other.rank.getPower());
        return compare != 0 ? compare : suit.compareTo(other.suit);
    }

    @Override
    @NonNull
    public String toString() {
        return rank.toString() + suit.toString();
    }
}
