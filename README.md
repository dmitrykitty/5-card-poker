
# 5-Card Draw Poker
A complete implementation of a multiplayer **5-Card Draw Poker** game in Java. The project follows a Client-Server architecture and features a robust game engine capable of handling complex scenarios like side pots, split pots, and player disconnects.

## Table of Contents
1. [About the Project](https://www.google.com/search?q=%23-about-the-project)
2. [Features](https://www.google.com/search?q=%23-features)
3. [System Architecture](https://www.google.com/search?q=%23-system-architecture)
4. [Diagrams](https://www.google.com/search?q=%23-diagrams)
5. [Project Structure](https://www.google.com/search?q=%23-project-structure)
6. [Installation & Setup](https://www.google.com/search?q=%23-installation--setup)
7. [Technologies](https://www.google.com/search?q=%23-technologies)

---

## About the Project

This project is a simulation of a poker game for 2-4 players. It consists of an independent game engine (`poker-module`), a TCP server (`poker-server`), and a console-based client (`poker-client`).

The primary goal was to build a solid "Game Engine" that correctly handles complex poker rules and edge cases, such as:

* **Split Pots:** Fairly distributing chips when players have identical hands.
* **Side Pots:** Managing separate pots when a player goes All-In with fewer chips than others.
* **Fault Tolerance:** Handling player disconnects gracefully (Auto-Fold / Sitting Out logic).

---

## Features
### Game Engine 
* **Full State Machine:** LOBBY -> ANTE -> DEALING -> BETTING 1 -> DRAWING -> BETTING 2 -> SHOWDOWN -> FINISHED.
* **Advanced Pot Manager:** Handles multiple pots (Main + Side) and mathematically correct split calculations.
* **Dealer & Deck:** Shuffling, dealing, and exchanging cards logic.
* **Hand Evaluator:** Categorizes poker hands (from High Card to Royal Flush) and compares them using kickers.

### Network & Security 
* **Multi-threaded Server:** Each client is handled in a separate `ClientHandler` thread.
* **Text Protocol:** Custom command-based protocol (e.g., `BET 100`, `FOLD`, `DRAW 1 3`).
* **Security & Validation:**
* `RateLimiter`: Protects against command spamming.
* `TimeoutManager`: Disconnects inactive (AFK) players.
* **Move Validation:** Ensures actions are only allowed during the correct turn and game state.



---

## System ArchitectureThe project is divided into 4 Maven modules to ensure separation of concerns:

1. **`poker-common`**: Shared data models (`Card`, `Rank`, `Suit`), events (`GameEvent`), and network protocol definitions.
2. **`poker-module`**: The "Brain" of the operation. Contains pure business logic. It knows nothing about the network or the UI.
3. **`poker-server`**: The Network Layer. Translates TCP messages into Game Engine calls (`Table`) and broadcasts events (`GameObserver`) back to clients.
4. **`poker-client`**: A Console User Interface (TUI) for the players.

---


## Installation & Setup
### Requirements
* Java JDK 21 or higher
* Maven 3.6+

### Step 1: Build the ProjectRun the following command in the root directory:

```bash
mvn clean install

```

*This will compile all modules and run unit & integration tests (including `TableIntegrationTest` which verifies game scenarios).*

### Step 2: Run the Server
```bash
cd poker-server
mvn exec:java
```

The server will start on port `5000` by default.


### Step 3: Run the ClientsOpen a new terminal window for each player (minimum 2 players required).

```bash
cd poker-client
mvn exec:java

```

Follow the on-screen instructions:

1. Enter your username.
2. Type `CREATE` to start a new table (first player).
3. Type `JOIN <gameId>` to join an existing table (subsequent players).
4. Type `START` when everyone is ready (table owner only).

---

## Technologies
* **Language:** Java 21+
* **Build Tool:** Maven
* **Testing:** JUnit 5, Mockito, AssertJ
* **Logging:** SLF4J + Logback (with file rotation and console coloring).
* **Concurrency:** `ReentrantLock` (Game state safety), `ConcurrentHashMap`, Virtual Threads / Thread Pool.
* **Boilerplate:** Lombok.

