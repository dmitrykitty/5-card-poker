# 5-Card Draw Poker | Multiplayer Game Engine

A robust, modular, and scalable implementation of the classic **5-Card Draw Poker** game. This project features a multi-threaded server architecture using Java 21 Virtual Threads, a pure domain-driven game engine, and a custom text-based network protocol.

## Project Demonstration

[](https://www.google.com/search?q=YOUR_VIDEO_URL_HERE)
*(Replace the URL above with your recorded demonstration)*

---

## System Architecture

The project is designed using a **Multi-Module Maven Architecture** to ensure a strict separation of concerns (SoC).

| Module             | Responsibility                                                                                                |
|--------------------|---------------------------------------------------------------------------------------------------------------|
| **`poker-common`** | Shared DTOs, Enums (`Rank`, `Suit`), and the **Sealed Interface** `GameEvent` system.                         |
| **`poker-module`** | Core Domain Logic. Contains the Game Engine, Hand Evaluation, and Pot Management. Zero external dependencies. |
| **`poker-server`** | The Infrastructure Layer. Handles TCP Sockets, Virtual Thread management, and Security.                       |
| **`poker-client`** | A Terminal User Interface (TUI) providing real-time game state rendering and user input handling.             |

---

## Design Patterns

This project serves as a practical application of several fundamental software engineering patterns:

### 1. Strategy Pattern (`HandEvaluator`)

The evaluation logic is decoupled from the `Table` using the **Strategy Pattern**. The `HandEvaluator` interface defines the contract, while `StandardFiveCardHandEvaluator` provides the specific algorithm for 5-card draw rules. This allows the system to easily support other poker variants (like Texas Hold'em) just by swapping the strategy.

### 2. Observer Pattern (`GameObserver`)

To decouple the **Game Engine** from the **Network Layer**, we implemented the **Observer Pattern**. The `Table` acts as the *Subject*, and `ClientHandler` acts as the *Observer*. When a game event occurs (e.g., `CardsDealt`, `TurnChanged`), the engine notifies all observers without knowing how the information is transmitted to the client.

### 3. Factory Pattern (`GameFactory`)

The initialization of a poker table is managed by the `GameFactory`. This ensures that all components (Deck, Evaluator, Config) are created in a consistent state. The `FiveCardDrawFactory` encapsulates the specific rules and starting parameters for this game mode.

### 4. Singleton Pattern (`GameManager`)

The `GameManager` uses a thread-safe Singleton pattern to provide a centralized registry for all active games on the server, ensuring that clients can reliably find and join specific tables.

### 5. Facade Pattern (`Table`)

The `Table` class acts as a Facade, orchestrating complex interactions between lower-level components like the `Dealer`, `PotManager`, and `TurnOrder`, providing a simple API for the Server layer to interact with the game.

---

## Key Technical Highlights

### Java 21 Virtual Threads

The server utilizes the `newVirtualThreadPerTaskExecutor()`. This allows the application to handle thousands of concurrent players with a simple "thread-per-client" model. Even though network I/O is blocking, Virtual Threads are unmounted from carrier threads during wait times, ensuring massive scalability without the complexity of asynchronous NIO code.

### Advanced Pot Management

The `PotManager` handles complex **Side Pot** and **Split Pot** scenarios. It uses a "horizontal slicing" algorithm to create discrete pots when players go **All-In** with different stack sizes, ensuring mathematical fairness in payouts.

### Security & Validation

* **Rate Limiting:** Prevents DoS attacks by limiting the number of commands per second per client.
* **Timeout Management:** Automatically folds inactive players to prevent "stuck" game states.
* **Input Sanitization:** Uses a `ConnectionValidator` to block malicious payloads and protocol injection.
* **Information Hiding:** Opponent cards are masked as `HIDDEN` at the protocol level; only the owner sees their actual cards.

---

## Installation & Running

### Prerequisites

* **JDK 21+** (required for Virtual Threads and Records)
* **Maven 3.8+**

### Building

```bash
mvn clean install

```

### Execution

1. **Start Server:**
   Run `scripts/run_server.ps1` (Windows) or `scripts/run_server.sh` (Linux/Mac).
2. **Start Client:**
   Run `scripts/run_client.ps1` (Windows) or `scripts/run_client.sh` (Linux/Mac).

---

## Quality Assurance

* **Unit Testing:** Over 70% code coverage using **JUnit 5** and **Mockito**.
* **Static Analysis:** Configured for **SonarQube** and **JaCoCo** to maintain high code standards.
* **Logging:** Structured logging via **SLF4J/Logback** with separate files for server and client.

---

## Technology Stack

* **Language:** Java 21 (Records, Sealed Classes, Virtual Threads)
* **Build Tool:** Maven
* **Testing:** JUnit 5, AssertJ, Mockito, Awaitility
* **Utility:** Project Lombok
* **Environment:** Docker (for SonarQube analysis)
