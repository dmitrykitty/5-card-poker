package com.dnikitin.poker.common.model.events;

/**
 * Defines the contract for components that wish to listen to game state changes.
 * <p>
 * This interface implements the <b>Observer Pattern</b>. It decouples the Game Engine (Observable)
 * from the Network Layer (Observer). The engine publishes {@link GameEvent}s without knowing
 * who is listening or how those events are processed (e.g., sent over TCP, logged to a file, or displayed in a GUI).
 * </p>
 */
public interface GameObserver {

    /**
     * Called by the observable (e.g., Table) when a significant event occurs.
     *
     * @param event The typed event containing details about the state change.
     */
    void onGameEvent(GameEvent event);
}