package com.dnikitin.poker.gamelogic;

import com.dnikitin.poker.common.model.Card;
import com.dnikitin.poker.model.HandResult;

import java.util.List;

public interface HandEvaluator {

    HandResult evaluate(List<Card> cards);
}
