package com.dnikitin.poker.model;


import com.dnikitin.poker.common.model.game.Card;
import com.dnikitin.poker.common.model.game.HandRank;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
public class HandResult implements Comparable<HandResult> {
    private final HandRank handRank;
    private final List<Card> mainCards;
    private final List<Card> otherCards;

    @Builder
    public HandResult(HandRank handRank, List<Card> mainCards, List<Card> otherCards) {
        this.handRank = handRank;
        this.mainCards = mainCards != null ? List.copyOf(mainCards) : List.of();
        this.otherCards = otherCards != null ? List.copyOf(otherCards) : List.of();
    }

    @Override
    public int compareTo(HandResult other) {
        int handRankComparison = Integer.compare(handRank.getStrength(), other.getHandRank().getStrength());

        if (handRankComparison != 0) {
            return handRankComparison;
        }
        int mainCardsComparison = compareCardLists(mainCards, other.getMainCards());

        if (mainCardsComparison != 0) {
            return mainCardsComparison;
        }
        return compareCardLists(otherCards, other.otherCards);
    }

    public List<Card> getAllCards() {
        List<Card> combined = new ArrayList<>(mainCards);
        combined.addAll(otherCards);
        return Collections.unmodifiableList(combined); //read-only
    }

    private int compareCardLists(List<Card> mainCards, List<Card> otherCards) {
        int size = Math.min(mainCards.size(), otherCards.size());
        int result = 0;

        for (int i = 0; i < size; i++) {
            result = mainCards.get(i).compareByPowerOnly(otherCards.get(i));
            if (result != 0) {
                return result;
            }
        }
        return result;
    }
}
