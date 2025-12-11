package com.dnikitin.poker.game;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class GameManager {
    // Singleton instance
    private static final GameManager INSTANCE = new GameManager();

    private final Map<String, Table> activeGames = new ConcurrentHashMap<>();

    private final GameFactory defaultFactory;

    public GameManager() {
        defaultFactory = new FiveCardDrawFactory();
    }

    public static GameManager getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new game table and registers it.
     *
     * @return The ID of the newly created game.
     */
    public String createGame() {
        Table table = new Table(defaultFactory);
        activeGames.put(table.getId(), table);
        log.info("Created new game table: {}. Active games: {}", table.getId(), activeGames.size());
        return table.getId();
    }

    /**
     * Finds an active game by its ID.
     *
     * @param gameId The UUID of the game.
     * @return Optional containing the Table if found.
     */
    public Optional<Table> findGame(String gameId) {
        return Optional.ofNullable(activeGames.get(gameId));
    }

    /**
     * Removes a game from the registry (e.g. when finished).
     */
    public void removeGame(String gameId) {
        activeGames.remove(gameId);
        log.info("Removed game table: {}. Active games: {}", gameId, activeGames.size());
    }
}
