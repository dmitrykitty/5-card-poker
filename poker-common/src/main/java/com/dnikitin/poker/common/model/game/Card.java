package com.dnikitin.poker.common.model.game;

import lombok.NonNull;

public record Card(Rank rank, Suit suit) implements Comparable<Card> {


    @Override

    public int compareTo(@NonNull Card other) {
        int compare = Integer.compare(rank.getPower(), other.rank.getPower());
        return compare != 0 ? compare : suit.compareTo(other.suit);
    }

    public int compareByPowerOnly(@NonNull Card other){
        return Integer.compare(rank.getPower(), other.rank.getPower());
    }

    @Override
    @NonNull
    public String toString() {
        return rank.toString() + suit.toString();
    }
}
