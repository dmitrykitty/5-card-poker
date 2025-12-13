package com.dnikitin.poker.common.model.events;

public interface GameObserver {
    void onGameEvent(GameEvent event);
}
