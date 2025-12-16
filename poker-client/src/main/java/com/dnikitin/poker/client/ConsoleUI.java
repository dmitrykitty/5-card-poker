package com.dnikitin.poker.client;

import java.util.*;

public class ConsoleUI {

    private static final String BORDER_HOR = "═";
    private static final String BORDER_VER = "║";
    private static final String CORNER_TL = "╔";
    private static final String CORNER_TR = "╗";
    private static final String CORNER_BL = "╚";
    private static final String CORNER_BR = "╝";
    private static final String SEP_L = "╠";
    private static final String SEP_R = "╣";

    private static final int WIDTH = 50;

    public void printDashboard(ClientGameState state) {
        System.out.println(); // Odstęp
        printLine(CORNER_TL, CORNER_TR);

        printRow(" Phase: " + state.getCurrentPhase());
        printRow(" Pot:   " + state.getCurrentPot());

        printLine(SEP_L, SEP_R);

        // ZMIANA: Używamy getMyName() zamiast getName()
        // Metoda getMyName() sama dba o zwrócenie "Unknown" lub ID w razie braku imienia
        printRow(" Player: " + state.getMyName());

        // ZMIANA: Używamy getMyChips() zamiast getChips() (pobiera z mapy)
        printRow(" Chips:  " + state.getMyChips());

        printRow(" To Call: " + state.getAmountToCall());

        printLine(SEP_L, SEP_R);

        // ZMIANA: Używamy getMyHand() zamiast getHand()
        String handStr = formatHand(state.getMyHand());
        printRow(" HAND: " + handStr);

        printLine(CORNER_BL, CORNER_BR);

        if (!state.getLastMessage().isEmpty()) {
            System.out.println(" > SYSTEM: " + state.getLastMessage());
        }
    }

    public void printMessage(String msg) {
        System.out.println(msg);
    }

    public void printError(String msg) {
        System.err.println(" [!] " + msg);
    }

    public void printHelp(ClientGameState state) {
        if (state.getGameId() == null || state.getPlayerId() == null) {
            printStartupHelp();
        } else {
            printGameHelp();
        }
    }


    private String formatHand(List<String> hand) {
        if (hand.isEmpty()) return "[ NO CARDS ]";
        StringBuilder sb = new StringBuilder();
        for (String card : hand) {
            sb.append("[").append(card).append("] ");
        }
        return sb.toString().trim();
    }

    private void printLine(String left, String right) {
        System.out.print(left);
        for (int i = 0; i < WIDTH; i++) System.out.print(BORDER_HOR);
        System.out.println(right);
    }

    private void printRow(String content) {
        System.out.print(BORDER_VER);
        String format = "%-" + WIDTH + "s";

        if (content.length() > WIDTH) content = content.substring(0, WIDTH);
        System.out.printf(format, content);
        System.out.println(BORDER_VER);
    }

    private void printStartupHelp() {
        System.out.println();
        printLine(CORNER_TL, CORNER_TR);
        printRow("              STARTUP COMMANDS");
        printLine(SEP_L, SEP_R);

        printRow(" create             - Create a new game");
        printRow(" join <id> <name>   - Join game (e.g. join 123 Alice)");
        printRow(" help               - Show this menu");
        printRow(" quit               - Exit application");

        printLine(CORNER_BL, CORNER_BR);
    }


    private void printGameHelp() {
        System.out.println();
        printLine(CORNER_TL, CORNER_TR);
        printRow("              AVAILABLE COMMANDS");

        printLine(SEP_L, SEP_R);

        printRow(" check           - Pass turn (if no bet)");
        printRow(" call            - Match current bet");
        printRow(" raise <amount>  - Increase bet (e.g. raise 50)");
        printRow(" fold            - Give up hand");
        printRow(" draw <indexes>  - Exchange cards (e.g. draw 0,2)");
        printRow(" help            - Show this menu");
        printRow(" quit            - Exit game");

        printLine(CORNER_BL, CORNER_BR);
    }

}