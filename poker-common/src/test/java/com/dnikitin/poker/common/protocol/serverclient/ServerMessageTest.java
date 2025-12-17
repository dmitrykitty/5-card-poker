package com.dnikitin.poker.common.protocol.serverclient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ServerMessageTest {

    private final Map<String, String> testParams = Map.of(
            "POT", "1500",
            "NAME", "John_Doe",
            "REASON", "Invalid_input",
            "CARDS", "A♠,K♥,Q♣,J♦,10♠",
            "NONE_CARDS", "NONE",
            "EMPTY", ""
    );
    private final ServerMessage msg = new ServerMessage(ServerMessage.Type.ROUND, testParams);

    // --- Test getDecoded ---

    @Test
    @DisplayName("Should correctly decode text, replacing underscores with spaces")
    void testGetDecoded_Success() {
        assertThat(msg.getDecoded("NAME")).isEqualTo("John Doe");
        assertThat(msg.getDecoded("REASON")).isEqualTo("Invalid input");
    }

    @Test
    @DisplayName("Should return empty string for missing key (getDecoded)")
    void testGetDecoded_MissingKey() {
        assertThat(msg.getDecoded("MISSING")).isEqualTo("");
    }

    // --- Test getInt ---

    @Test
    @DisplayName("Should correctly parse existing integer")
    void testGetInt_Success() {
        assertThat(msg.getInt("POT")).isEqualTo(1500);
    }

    @Test
    @DisplayName("Should return default value for missing key")
    void testGetInt_MissingKey() {
        assertThat(msg.getInt("MISSING_POT", 99)).isEqualTo(99);
        assertThat(msg.getInt("MISSING_POT")).isEqualTo(0); // Testujemy getInt() bez drugiego argumentu
    }

    @Test
    @DisplayName("Should return default value for invalid format (non-numeric)")
    void testGetInt_InvalidFormat() {
        assertThat(msg.getInt("NAME", 50)).isEqualTo(50);
    }

    @Test
    @DisplayName("Should handle empty string as invalid format")
    void testGetInt_EmptyString() {
        assertThat(msg.getInt("EMPTY", 1)).isEqualTo(1);
    }

    // --- Test getList ---

    @Test
    @DisplayName("Should correctly parse a comma-separated list of cards")
    void testGetList_Success() {
        List<String> cards = msg.getList("CARDS");
        assertThat(cards).containsExactly("A♠", "K♥", "Q♣", "J♦", "10♠");
        assertThat(cards).hasSize(5);
    }

    @Test
    @DisplayName("Should return empty list for missing key")
    void testGetList_MissingKey() {
        assertThat(msg.getList("MISSING_CARDS")).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list if key value is NONE")
    void testGetList_None() {
        assertThat(msg.getList("NONE_CARDS")).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for empty string")
    void testGetList_EmptyString() {
        assertThat(msg.getList("EMPTY")).isEmpty();
    }
}
