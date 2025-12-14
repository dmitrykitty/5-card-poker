package com.dnikitin.poker.game;

import com.dnikitin.poker.exceptions.rules.IllegalPlayerAmountException;
import com.dnikitin.poker.exceptions.moves.WrongGameStateException;
import com.dnikitin.poker.game.setup.FiveCardDrawFactory;
import com.dnikitin.poker.game.setup.GameFactory;
import com.dnikitin.poker.game.state.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

public class TableTest {

    private Table table;
    private final GameFactory factory = new FiveCardDrawFactory();

    @BeforeEach
    void setUp() {
        table = new Table(factory);
    }

    @Test
    void testLobbyAndAnteStateStartCorrectly(){
        Player p1 = new Player("1", "P1", 100);
        Player p2 = new Player("2", "P2", 100);
        table.addPlayer(p1);
        table.addPlayer(p2);

        assertThat(table.getCurrentState()).isEqualTo(GameState.LOBBY);


        table.startGame();

        assertAll(
                () -> assertThat(table.getPot()).isEqualTo(20),
                () -> assertThat(p1.getChips()).isEqualTo(90),
                () -> assertThat(p2.getChips()).isEqualTo(90),
                () -> assertThat(p1.getCurrentBet()).isEqualTo(10),
                () -> assertThat(p2.getCurrentBet()).isEqualTo(10),
                () -> assertThat(table.getDeck().size()).isEqualTo(42),
                () -> assertThat(table.getCurrentState()).isEqualTo(GameState.BETTING_1)
        );
    }

    @Test
    void testThrowingExceptionsDuringStartOfTheGame(){
        Player p1 = new Player("1", "P1", 5);
        Player p2 = new Player("2", "P2", 100);
        Player p3 = new Player("3", "P3", 100);
        Player p4 = new Player("4", "P4", 100);
        Player p5 = new Player("5", "P5", 100);
        Player p6 = new Player("6", "P6", 100);
        assertThatThrownBy(() -> table.startGame())
                .isInstanceOf(IllegalPlayerAmountException.class)
                .hasMessageContaining("Not enough players to start.");

        table.addPlayer(p1);
        table.addPlayer(p2);

        assertThatThrownBy(() -> table.startGame())
                .isInstanceOf(IllegalPlayerAmountException.class)
                .hasMessageContaining("Not enough players after Ante collection");


    }

    @Test
    void testThrowingExceptionWhenToManyPlayersOrWrongState(){
        Player p1 = new Player("1", "P1", 100);
        Player p2 = new Player("2", "P2", 100);
        Player p3 = new Player("3", "P3", 100);
        Player p4 = new Player("4", "P4", 100);
        Player p5 = new Player("5", "P5", 100);
        Player p6 = new Player("6", "P6", 100);

        table.addPlayer(p1);
        table.addPlayer(p2);
        table.addPlayer(p3);
        table.addPlayer(p4);

        assertThatThrownBy(() -> table.addPlayer(p5))
                .isInstanceOf(IllegalPlayerAmountException.class)
                .hasMessageContaining("Table is full");

        table.startGame();

        assertThatThrownBy(() -> table.addPlayer(p1))
                .isInstanceOf(WrongGameStateException.class)
                .hasMessageContaining("Game already started");
    }

    @Test
    void testPassTurnAfterFold() {
        Player p1 = new Player("1", "Alice", 1000);
        Player p2 = new Player("2", "Bob", 1000);
        table.addPlayer(p1);
        table.addPlayer(p2);
        table.startGame();

        Player firstToAct = table.getCurrentPlayer();
        Player secondToAct = firstToAct == p1 ? p2 : p1;

        table.playerFold(firstToAct);

        assertAll(
                () -> assertThat(firstToAct.isFolded()).isTrue(),
                () -> assertThat(table.getCurrentPlayer()).isEqualTo(secondToAct)
        );
    }
}
