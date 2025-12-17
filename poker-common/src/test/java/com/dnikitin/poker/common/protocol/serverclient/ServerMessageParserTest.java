package com.dnikitin.poker.common.protocol.serverclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServerMessageParserTest {

    private ServerMessageParser parser;

    @BeforeEach
    void setUp() {
        parser = new ServerMessageParser();
    }

    @Test
    @DisplayName("Should parse STATE message")
    void testParseState() {
        ServerMessage msg = parser.parse("STATE PHASE=BETTING_1");

        assertThat(msg.type()).isEqualTo(ServerMessage.Type.STATE);
        assertThat(msg.get("PHASE")).hasValue("BETTING_1");
    }

    @Test
    @DisplayName("Should parse DEAL message")
    void testParseDeal() {
        ServerMessage msg = parser.parse("DEAL PLAYER=p-1 CARDS=A♠,K♥");

        assertThat(msg.type()).isEqualTo(ServerMessage.Type.DEAL);
        assertThat(msg.get("PLAYER")).hasValue("p-1");
        assertThat(msg.get("CARDS")).hasValue("A♠,K♥");
    }

    @Test
    @DisplayName("Should parse numeric parameters correctly")
    void testParseIntParams() {
        ServerMessage msg = parser.parse("ROUND POT=500 HIGHESTBET=100");

        assertThat(msg.type()).isEqualTo(ServerMessage.Type.ROUND);
        assertThat(msg.getInt("POT", 0)).isEqualTo(500);
        assertThat(msg.getInt("HIGHESTBET", 0)).isEqualTo(100);
        assertThat(msg.getInt("NON_EXISTENT", -1)).isEqualTo(-1);
    }

    @Test
    @DisplayName("Should handle unknown message types gracefully")
    void testParseUnknown() {
        ServerMessage msg = parser.parse("WTF PARAM=VALUE");

        assertThat(msg.type()).isEqualTo(ServerMessage.Type.UNKNOWN);
    }

    @Test
    @DisplayName("Should handle empty or null input")
    void testParseEmpty() {
        assertThat(parser.parse(null).type()).isEqualTo(ServerMessage.Type.UNKNOWN);
        assertThat(parser.parse("   ").type()).isEqualTo(ServerMessage.Type.UNKNOWN);
    }
}
