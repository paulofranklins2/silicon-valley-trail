# Silicon Valley Trail

Turn-based survival game inspired by Oregon Trail, but in a startup world.

You start in "The Garage" and try to make it to "The IPO" without running out of money, morale, or completely destroying your codebase.

---

## Idea

Each turn you make decisions like:
- work on product
- pitch VCs
- travel to next location
- rest

Every decision affects your resources:
- capital
- morale
- technical debt
- hype

There are also events influenced by real-world data (like weather or market), so the game is not fully predictable.

The goal is to survive and reach IPO.

---

## Project Structure

```text
src/main/java/com/paulocunha/svt

  domain/
    GameState
    TeamState
    ResourceState
    GameAction
    GameEvent

  application/
    GameEngine
    TurnProcessor
    ActionHandler
    EventProcessor

  infrastructure/
    api/
      ExternalSignalProvider
      WeatherApiClient
      MockProvider
    persistence/
      SaveGameRepository
      JsonSaveGameRepository

  presentation/
    cli/
      ConsoleRenderer
      ConsoleInputHandler

  Main.java
````

---

## Notes

* Built with Java 21 + Maven
* CLI-based.
* API failures are handled with fallback (game still runs offline)
* Designed so it can be extended later (web, multiplayer, database, etc)

---

## How to run


```bash
COMING SOON
```
