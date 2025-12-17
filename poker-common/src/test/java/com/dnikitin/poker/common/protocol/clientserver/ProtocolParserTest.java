package com.dnikitin.poker.common.protocol.clientserver;

import com.dnikitin.poker.common.exceptions.ProtocolException;
import com.dnikitin.poker.common.protocol.clientserver.commands.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtocolParserTest {

    private ProtocolParser parser;

    @BeforeEach
    void setUp() {
        parser = new ProtocolParser();
    }

    // --- INITIAL COMMANDS ---

    @Test
    @DisplayName("Should parse HELLO command")
    void testParseHello() {
        Command cmd = parser.parse("HELLO VERSION=2.5");

        assertThat(cmd).isInstanceOf(HelloCommand.class);
        assertThat(((HelloCommand) cmd).getVersion()).isEqualTo("2.5");
    }

    @Test
    @DisplayName("Should parse CREATE command with defaults")
    void testParseCreate() {
        Command cmd = parser.parse("CREATE ANTE=20 BET=50");

        assertThat(cmd).isInstanceOf(CreateCommand.class);
        CreateCommand create = (CreateCommand) cmd;
        assertThat(create.getAnte()).isEqualTo(20);
        assertThat(create.getBet()).isEqualTo(50);
        assertThat(create.getLimit()).isEqualTo("FIXED"); // Default
    }

    @Test
    @DisplayName("Should parse JOIN command")
    void testParseJoin() {
        Command cmd = parser.parse("JOIN GAME=g-123 NAME=SuperPlayer");

        assertThat(cmd).isInstanceOf(JoinCommand.class);
        JoinCommand join = (JoinCommand) cmd;
        assertThat(join.getGameId()).isEqualTo("g-123");
        assertThat(join.getName()).isEqualTo("SuperPlayer");
    }

    @Test
    @DisplayName("Should throw exception when JOIN misses parameters")
    void testParseJoinMissingParams() {
        assertThatThrownBy(() -> parser.parse("JOIN GAME=123"))
                .isInstanceOf(ProtocolException.class)
                .hasMessageContaining("JOIN requires GAME and NAME");
    }

    // --- IN-GAME COMMANDS ---

    @Test
    @DisplayName("Should parse simple commands (CHECK, CALL, FOLD)")
    void testParseSimpleCommands() {
        Command cmd = parser.parse("game-1 p-1 CHECK");

        assertThat(cmd).isInstanceOf(SimpleCommand.class);
        assertThat(cmd.getType()).isEqualTo(Command.CommandType.CHECK);
        assertThat(cmd.getGameId()).isEqualTo("game-1");
        assertThat(cmd.getPlayerId()).isEqualTo("p-1");
    }

    @Test
    @DisplayName("Should parse RAISE command")
    void testParseRaise() {
        Command cmd = parser.parse("game-1 p-1 RAISE AMOUNT=150");

        assertThat(cmd).isInstanceOf(BetCommand.class);
        BetCommand bet = (BetCommand) cmd;
        assertThat(bet.getType()).isEqualTo(Command.CommandType.RAISE);
        assertThat(bet.getAmount()).isEqualTo(150);
    }

    @Test
    @DisplayName("Should throw exception if AMOUNT is missing or invalid")
    void testParseBetInvalid() {
        assertThatThrownBy(() -> parser.parse("game-1 p-1 BET"))
                .isInstanceOf(ProtocolException.class);

        assertThatThrownBy(() -> parser.parse("game-1 p-1 BET AMOUNT=lots"))
                .isInstanceOf(ProtocolException.class);
    }

    @Test
    @DisplayName("Should parse DRAW command with indexes")
    void testParseDraw() {
        Command cmd = parser.parse("game-1 p-1 DRAW CARDS=0,2,4");

        assertThat(cmd).isInstanceOf(DrawCommand.class);
        DrawCommand draw = (DrawCommand) cmd;
        assertThat(draw.getCardIndexes()).containsExactly(0, 2, 4);
    }

    @Test
    @DisplayName("Should parse DRAW command with NONE")
    void testParseDrawNone() {
        Command cmd = parser.parse("game-1 p-1 DRAW CARDS=NONE");

        assertThat(cmd).isInstanceOf(DrawCommand.class);
        assertThat(((DrawCommand) cmd).getCardIndexes()).isEmpty();
    }

    // --- EDGE CASES ---

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("Should throw exception on empty input")
    void testEmptyInput(String input) {
        assertThatThrownBy(() -> parser.parse(input))
                .isInstanceOf(ProtocolException.class)
                .hasMessageContaining("Message is empty");
    }

    @Test
    @DisplayName("Should throw exception on message too large")
    void testMessageTooLarge() {
        String hugeMessage = "A".repeat(600);
        assertThatThrownBy(() -> parser.parse(hugeMessage))
                .isInstanceOf(ProtocolException.class)
                .hasMessageContaining("Message exceeds maximum size");
    }

    @Test
    @DisplayName("Should throw exception on unknown command")
    void testUnknownCommand() {
        assertThatThrownBy(() -> parser.parse("game1 p1 DANCE"))
                .isInstanceOf(ProtocolException.class)
                .hasMessageContaining("Unknown action: DANCE");
    }
}
