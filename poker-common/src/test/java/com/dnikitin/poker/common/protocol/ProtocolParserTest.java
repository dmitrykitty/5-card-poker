package com.dnikitin.poker.common.protocol;

import com.dnikitin.poker.common.exceptions.ProtocolException;
import com.dnikitin.poker.common.protocol.commands.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolParserTest {

    private ProtocolParser parser;

    @BeforeEach
    void setUp() {
        parser = new ProtocolParser();
    }

    @Test
    void testParseHelloCommand() {
        Command cmd = parser.parse("HELLO VERSION=1.0");

        assertInstanceOf(HelloCommand.class, cmd);
        HelloCommand hello = (HelloCommand) cmd;
        assertEquals("1.0", hello.getVersion());
    }

    @Test
    void testParseCreateCommand() {
        Command cmd = parser.parse("CREATE ANTE=10 BET=20 LIMIT=FIXED");

        assertInstanceOf(CreateCommand.class, cmd);
        CreateCommand create = (CreateCommand) cmd;
        assertEquals(10, create.getAnte());
        assertEquals(20, create.getBet());
        assertEquals("FIXED", create.getLimit());
    }

    @Test
    void testParseJoinCommand() {
        Command cmd = parser.parse("JOIN GAME=abc123 NAME=Player1");

        assertInstanceOf(JoinCommand.class, cmd);
        JoinCommand join = (JoinCommand) cmd;
        assertEquals("abc123", join.getGameId());
        assertEquals("Player1", join.getName());
    }

    @Test
    void testParseSimpleCommands() {
        Command checkCmd = parser.parse("game1 player1 CHECK");
        assertInstanceOf(SimpleCommand.class, checkCmd);
        assertEquals(Command.CommandType.CHECK, checkCmd.getType());

        Command foldCmd = parser.parse("game1 player1 FOLD");
        assertInstanceOf(SimpleCommand.class, foldCmd);
        assertEquals(Command.CommandType.FOLD, foldCmd.getType());

        Command callCmd = parser.parse("game1 player1 CALL");
        assertInstanceOf(SimpleCommand.class, callCmd);
        assertEquals(Command.CommandType.CALL, callCmd.getType());
    }

    @Test
    void testParseBetCommand() {
        Command cmd = parser.parse("game1 player1 BET AMOUNT=50");

        assertInstanceOf(BetCommand.class, cmd);
        BetCommand bet = (BetCommand) cmd;
        assertEquals("game1", bet.getGameId());
        assertEquals("player1", bet.getPlayerId());
        assertEquals(50, bet.getAmount());
    }

    @Test
    void testParseRaiseCommand() {
        Command cmd = parser.parse("game1 player1 RAISE AMOUNT=100");

        assertInstanceOf(BetCommand.class, cmd);
        BetCommand raise = (BetCommand) cmd;
        assertEquals(Command.CommandType.RAISE, raise.getType());
        assertEquals(100, raise.getAmount());
    }

    @Test
    void testParseDrawCommand() {
        Command cmd = parser.parse("game1 player1 DRAW CARDS=0,2,4");

        assertInstanceOf(DrawCommand.class, cmd);
        DrawCommand draw = (DrawCommand) cmd;
        assertEquals(3, draw.getCardIndexes().size());
        assertTrue(draw.getCardIndexes().contains(0));
        assertTrue(draw.getCardIndexes().contains(2));
        assertTrue(draw.getCardIndexes().contains(4));
    }

    @Test
    void testParseDrawCommandNone() {
        Command cmd = parser.parse("game1 player1 DRAW CARDS=NONE");

        assertInstanceOf(DrawCommand.class, cmd);
        DrawCommand draw = (DrawCommand) cmd;
        assertTrue(draw.getCardIndexes().isEmpty());
    }

    @Test
    void testParseEmptyMessageThrowsException() {
        assertThrows(ProtocolException.class, () -> parser.parse(""));
        assertThrows(ProtocolException.class, () -> parser.parse("   "));
        assertThrows(ProtocolException.class, () -> parser.parse(null));
    }

    @Test
    void testParseMessageTooLargeThrowsException() {
        String longMessage = "COMMAND " + "A".repeat(600);
        assertThrows(ProtocolException.class, () -> parser.parse(longMessage));
    }

    @Test
    void testParseUnknownCommandThrowsException() {
        assertThrows(ProtocolException.class,
            () -> parser.parse("game1 player1 UNKNOWN_ACTION"));
    }

    @Test
    void testParseMissingParametersThrowsException() {
        assertThrows(ProtocolException.class,
            () -> parser.parse("JOIN GAME=abc123")); // Missing NAME

        assertThrows(ProtocolException.class,
            () -> parser.parse("game1 player1 BET")); // Missing AMOUNT

        assertThrows(ProtocolException.class,
            () -> parser.parse("game1 player1 DRAW")); // Missing CARDS
    }

    @Test
    void testParseInvalidIntegerThrowsException() {
        assertThrows(ProtocolException.class,
            () -> parser.parse("game1 player1 BET AMOUNT=abc"));

        assertThrows(ProtocolException.class,
            () -> parser.parse("CREATE ANTE=xyz BET=10"));
    }

    @Test
    void testParseInvalidFormatThrowsException() {
        // Standard commands need GAME_ID PLAYER_ID ACTION
        assertThrows(ProtocolException.class,
            () -> parser.parse("CHECK")); // Missing game and player IDs
    }

    @Test
    void testParseCaseInsensitive() {
        Command cmd1 = parser.parse("hello version=1.0");
        assertInstanceOf(HelloCommand.class, cmd1);

        Command cmd2 = parser.parse("Game1 Player1 check");
        assertInstanceOf(SimpleCommand.class, cmd2);
        assertEquals(Command.CommandType.CHECK, cmd2.getType());
    }
}
