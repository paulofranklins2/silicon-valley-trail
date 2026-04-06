# Silicon Valley Trail - Design Plan

## 1. Language Selection

### Options Considered

| Language | Pros | Cons |
|---|---|---|
| **Java** | Strong type system, widely used in enterprise, great testing ecosystem | More verbose, slower to iterate |
| **Python** | Very fast to build, minimal setup, easy for prototyping | Weak type safety, less structured for larger systems |
| **TypeScript/Node** | Modern, good type safety, fast iteration | Dependency heavy |

### Decision: Java

**Why:** I chose Java because it's the language I'm strongest in. I've used it both during my internship at LinkedIn and before that, so I'm comfortable building something clean, structured, and testable within a short timeframe.

Python would be faster to prototype, and TypeScript is also a solid option, but Java gives me the best balance between speed and showing how I design and structure code.

---

## 2. Java Version

### Options Considered

| Version | Pros | Cons |
|---|---|---|
| **Java 8** | Universal compatibility | Missing modern features (records, pattern matching) |
| **Java 17 (LTS)** | Widely adopted, stable | Missing some newer features |
| **Java 21 (LTS)** | modern features, virtual threads | Slightly newer adoption |

### Decision: Java 21

**Why:** It's the current LTS and gives me useful features like records, sealed classes, and pattern matching. These help reduce boilerplate and make the code cleaner.

Java 8 felt too outdated for this, and Java 17 is fine, but 21 gives better tools with no real downside.

---

## 3. Build Tool

### Options Considered

| Tool | Pros | Cons |
|---|---|---|
| **Maven** | Predictable, widely understood | Verbose XML |
| **Gradle** | Flexible, concise | More complex, harder to debug |

### Decision: Maven

**Why:** This project is simple. I have a few dependencies and no custom build logic.

Maven fits perfectly here. It's predictable and easy to read.

Gradle would work, but I'd just be adding flexibility I don't need. Maven is the boring choice, and boring is correct here.

---

## 4. Framework & Presentation

### Decision: Spring Boot + Thymeleaf

**Why:** I wanted something visual without adding a full frontend project.

Opening `localhost:8080` and immediately seeing the game makes a big difference compared to a CLI. It shows the experience clearly.

Thymeleaf keeps everything in one project:
- No React
- No npm
- No extra build step

Just run the app and open the browser.

Each turn is just a form submit:
- user clicks action
- controller processes it
- page re-renders

That maps naturally to a turn-based game.

### Templates

3 templates:

| Template | What it shows |
|---|---|
| `start.html` | Team name input, begin game |
| `game.html` | Stats, location, progress, weather, action buttons, last turn result and event |
| `end.html` | Win or lose screen with final stats |

Plain HTML with Thymeleaf attributes. Turn results and events are shown inline on the game page instead of a separate page, so the player stays on one screen during gameplay.

---

### Why not SPA (React)

A SPA would look nicer, but it doubles the work:
- backend + frontend
- build tools
- API design

With 7 days, that's not worth it.

One project done well is better than two half-done.

---

## 5. Architecture

### Decision: Lightweight Hexagonal Architecture

**Why:** The main challenge here is dealing with external APIs while keeping the core logic stable.

I structured it like this:
- **Domain:** game rules and state
- **Ports:** what the game needs (weather, distance, persistence)
- **Adapters:** how those are implemented (API, mock, DB, web)

This gives me:
- easy testing (mock everything)
- clean separation from APIs
- ability to swap UI (web to CLI) without touching the core

I didn't go overboard with it, just enough to keep boundaries clean.

I didn't use MVC on its own because it doesn't really enforce boundaries between the domain and external systems. The Spring `@Controller` is just one adapter, not the center of the design.

I didn't use ECS because I only have one team and a handful of stats. ECS is for large simulations with many entities. Here it would be way more than I need.

---

## 6. Domain Model Design

### Decision: Composition with value objects

I split the state into:
- `TeamState`: health (0-100), energy (0-100), morale (0-100)
- `ResourceState`: cash, food, compute credits (floor at 0)
- `JourneyState`: current location, distance to next
- `GameState`: composes the three above, plus turn number, game-over flag, last event, last action, team name

Each one owns its own rules.

Example:
- `changeHealth(-15)` clamps to 0 internally
- no need for checks everywhere else

This keeps bugs contained and logic easier to reason about.

`lastEvent`, `lastAction`, and `lastActionResult` are stored so the web layer can show the player what happened on the previous turn. `lastActionResult` is an enum (`ActionEventResult`) that tracks the random outcome of scavenge (FOOD or CASH) and pitch VCs (PITCH_SUCCESS or PITCH_FAILURE). This way the UI knows exactly what happened without guessing from stat changes.

---

### Stats

I used 6 stats instead of 3:
- health
- energy
- morale
- cash
- food
- compute credits

With only 3 stats, the game becomes too simple.

With 6, actions create real tradeoffs:
- travel drains multiple things
- rest helps some but costs others

That's what makes decisions actually matter.

### Loss Conditions

Currently implemented:

| Condition | What happens |
|---|---|
| Health hits 0 | Team collapses, game over (LossReason.POOR_HEALTH) |
| Morale hits 0 | Team quits, game over (LossReason.POOR_MORALE) |
| Food at 0 for 2 turns | Team starves, game over (LossReason.STARVATION) |
| Cash at 0 for 3 turns | Funding dries up, game over (LossReason.NO_CASH) |
| Reach San Francisco | Victory |

Victory takes priority. If the team arrives with 0 health, they still win.

The grace period counters (`turnWithoutFood`, `turnWithoutCash`) are tracked in GameState and reset when the resource is recovered. The `LossReason` enum is set by ConditionEvaluator and displayed on the end screen.

---

## 7. Actions

I implemented 5 actions:

| Action | Effect | Tradeoff |
|---|---|---|
| **Travel** | +distance, -energy, -food, -compute | Moves forward but drains everything |
| **Rest** | +health, +energy, -food, +morale | Recovers the team but costs food and a turn |
| **Scavenge** | -energy, chance of +food/+cash | Risky, might find nothing |
| **Hackathon** | +compute, -energy, -morale, -food | Builds tech resources but burns out the team |
| **Pitch VCs** | chance of +cash, chance of -morale | High risk/high reward |

More than 3 gives better gameplay without adding much complexity.

Each action has a clear role, so nothing overlaps.

---

## 8. Event System

### Decision: Category-based pool with random chance

Events are grouped into 5 categories (weather, team, market, location, tech) with 3 events each, 15 total.

Not every turn triggers an event. There's a 20% chance per turn. I decided this during development because having an event every single turn felt overwhelming and made the game feel like button clicking. With 20%, events feel like something that actually happened, not background noise.

The event pool currently uses flat random selection. Weighted selection based on weather and game state is supported by the architecture (the method takes GameState and WeatherSignal as parameters) but not implemented yet. Same for player choice events, the EventOutcome model exists but no events use it yet.

5 choice events are implemented across all categories (weather, team, market, location, tech). Each presents the player with two options that have different stat tradeoffs. The choice modal pauses the game until the player decides. 
Weighted event selection based on weather and game state is supported but not yet implemented.

---

## 9. API Integration

### Decision: Open-Meteo + Haversine + OSRM + OpenRouteService

**Weather (Open-Meteo):**
- no API key needed
- real weather data for real locations
- directly affects gameplay (rain to harder travel, heat to morale drop)
- weather effects only apply when traveling, not when resting. I added this restriction because without it, players could exploit clear weather by spamming REST to get free health and energy. Now resting is "indoors" and weather only hits you on the road.

**Distance (3 APIs, 4 game modes):**

I started with just Haversine for straight-line distances. Then added OSRM for driving distances. Then realized I could make each API a game mode, so I added OpenRouteService for walking distances.

| Mode | API | Key needed | Difficulty |
|---|---|---|---|
| Fast | Haversine (math) | No | Easy (~65 km) |
| Road | OSRM driving → ORS car fallback | No (ORS key optional) | Medium (~106 km) |
| Walking | ORS foot-walking | Yes (free tier) | Hard (~130 km) |

The `DistancePort` interface made this painless. Each mode is just a different adapter. GameEngine picks the right one based on the player's selection. Zero changes to game logic.

**Pre-computed distances:** Originally I calculated distances when the player clicked "Start." That meant API calls blocked game creation. OSRM's public server is slow and unreliable, so Road mode could hang for 15+ seconds.

I moved the computation to application startup. All distances are cached once when the server boots. Game creation is instant - just reads from the cache. If an API was down at startup, the player can retry from a modal without restarting the server.

**Batch requests:** Both OSRM and ORS support sending all 15 waypoints in a single request. I used this instead of 14 separate calls, which would have been painfully slow.

**Locale bug:** `String.format("%f")` uses locale-specific decimal separators. On non-US systems, coordinates get commas instead of dots, breaking the API URL. Fixed with `Locale.US` everywhere I format coordinates.

**Weather caching:** Weather is also pre-loaded at startup for all 15 cities and cached for 1 hour. During gameplay, `loadWeather()` reads from the in-memory cache, no API calls mid-game. After the TTL expires, the next request for that city refreshes the cache.

All 4 game modes work with no API key. Walking modes use the same road distances as their normal-speed equivalents but with a 0.4x speed multiplier, difficulty comes from game mechanics, not from a different API.

---

### Fallback Strategy

Each adapter follows the same pattern: try the API, fall back on failure.

- `OpenMeteoAdapter` → `MockWeatherAdapter`
- `OsrmDistanceAdapter` → `OpenRouteServiceAdapter` (car profile) → `HaversineDistanceAdapter`
- `OpenRouteServiceAdapter` → `HaversineDistanceAdapter`

The key design choice: when OSRM fails but ORS car succeeds, `usedFallback` stays `false` because the player still gets real driving distances. The flag only becomes `true` when we end up at Haversine (estimated distances). This matters for fair leaderboard rankings - if the mode used estimated distances, the game downgrades to Fast mode so the player doesn't compete unfairly.

The fallback chain passes through the `DistanceResult.usedFallback` flag from the innermost adapter. `DistancePort.calculateLegDistances()` is a default method on the interface, so Haversine and Mock get it for free. OSRM and ORS override it with batch calls.

If the player gets the fallback modal, they can retry (calls the API live) or accept Fast mode. The retry updates the cache, so subsequent games on that mode use real distances without another API call.

---

## 10. Map Design

15 real locations from San Jose to San Francisco:

1. San Jose
2. Santa Clara
3. Sunnyvale
4. Cupertino
5. Mountain View
6. Palo Alto
7. Menlo Park
8. Redwood City
9. San Carlos
10. San Mateo
11. Burlingame
12. Millbrae
13. South San Francisco
14. Daly City
15. San Francisco

I started with 10 (the requirement minimum) but added 5 more during balance tuning. More cities means more market opportunities and more travel turns, which makes the game feel less rushed. The locations are defined in `locations.yaml`, not hardcoded in Java.

Each has real lat/long coordinates used by both the weather API and distance calculations. In Fast mode, total journey is about 65 km. In Road mode, it's about 106 km.

---

## 11. Persistence & Leaderboard

### Decision: H2 + Spring Data JPA for leaderboard

I originally planned save/load game functionality with multiple JPA entities (SavedGame, TeamSnapshot, etc.). But during development I realized the game is short enough that saving mid-game isn't necessary. What players actually want is to see how they rank against each other.

So I pivoted to a leaderboard system instead. One entity, one table:

| Entity | Fields | What it stores |
|---|---|---|
| `LeaderboardEntry` | id, playerName, teamName, turns, victory, lastLocation, health, energy, morale, cash, food, computeCredits, score, gameMode, createdAt | One row per completed game |

The score is calculated by `ScoreCalculator` at submission time, not on every leaderboard view. It uses weighted factors:
- Victory gives a 1000-point base
- Turn efficiency rewards faster wins (up to 500 bonus)
- Stats are weighted: morale x3, health x2, energy x1 (harder to maintain = more points)
- Resources are weighted: food x3, cash x2, compute x1 (scarcer = more points)
- Losers get a journey progress bonus based on how far they got

The leaderboard is split by game mode (Fast vs Road) so the rankings are fair. Road mode is harder, so comparing scores across modes wouldn't make sense.

I went back and forth on whether market state and leaderboard submission should live in the HTTP session or on GameState. Originally I had them as session attributes, but that meant cleanup code in `POST /start` to clear stale data. Moving everything onto GameState was cleaner: new game = new object = everything resets automatically.

### Database Config

The app uses `${DB_URL:jdbc:h2:mem:svt}` in `application.yml`. The default is H2 in-memory, so reviewers can run the app with zero setup. To switch to Postgres, just set three env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).

I added `spring-dotenv` so the app reads a `.env` file at the project root if one exists. This way you can drop a `.env` with your Postgres credentials and the app picks them up automatically. The `.env` file is gitignored, and `.env.example` shows the format.

A `docker-compose.yml` is included for convenience. `docker compose up -d` starts a Postgres 16 container with the default credentials. No manual database setup needed.

I considered Spring profiles (`application-postgres.yml`) but decided against it. One `application.yml` with env var defaults is simpler. No flags to remember, no profile switching. Just set the env vars and run.

For production at scale, I'd add Flyway for schema migrations and a database index on the `score` column for O(1) leaderboard queries. With `ddl-auto: update` and a handful of entries, it's not needed here.

### OSRM Fallback & Fair Rankings

When a player selects Road mode, the OSRM adapter returns a `DistanceResult` that includes a `usedFallback` flag. If the OSRM public server is unavailable (it's free and rate-limited), the adapter falls back to Haversine distances and sets this flag to `true`.

When fallback is triggered, the game mode automatically downgrades from ROAD to FAST. This way the player's score goes into the Fast leaderboard, not the Road leaderboard. Without this, a player could select Road mode, get Haversine distances (shorter), and compete unfairly against real Road mode players.

The player sees a toast on game load explaining what happened: "Road data unavailable. Using estimated distances. Ranking as Fast mode."

---

## 12. Data-Driven Design

### Decision: YAML files for game content

Early on, events, markets, locations, and action effects were all hardcoded in Java using builder chains. EventProcessor alone was 290 lines of `.builder().title("...").cashChange(-20).build()`. It worked, but it was ugly, hard to edit, and mixed data with logic.

I moved everything to YAML files in `resources/data/`:

| File | What it defines |
|---|---|
| `actions.yaml` | All 5 actions: stat effects, energy costs, travel distance, compute penalty |
| `events.yaml` | All events grouped by category, with stat changes and choice outcomes |
| `markets.yaml` | All 5 market variants with purchase options |
| `locations.yaml` | All 15 cities with lat/long coordinates |

`GameDataLoader` handles deserialization. Jackson + `jackson-dataformat-yaml` does the heavy lifting. The domain models needed `@NoArgsConstructor` for Jackson, which meant dropping `final` fields. Worth it for the cleaner separation.

I also introduced a `StatType` enum to replace magic strings like `"hp"` and `"cpu"` in the YAML. Now if you typo a stat name in the YAML (`stat: HELTH`), Jackson fails at startup with a clear error instead of silently ignoring it.

For testing, `EventProcessor` and `ActionHandler` use factory methods (`EventProcessor.create(random)`, `ActionHandler.create(random)`) that load from YAML. Tests can also use the constructor directly with empty data for isolation.

The result: EventProcessor went from 290 lines to 57. ActionHandler lost 20+ constants. Adding a new event or city is a YAML edit, no recompile.

---

## 13. City Market System

### Decision: Voluntary market, not forced events

I originally had the market trigger automatically when arriving at a new city. It interrupted the game flow and felt annoying. Players should choose when to shop, not be forced into it.

So I made it a button. The player opens the market whenever they want, browses the options, buys what they need, or closes it. No turn consumed.

Each city gets a random market variant from 5 types (Supply Market, Tech District, Food Truck Rally, Startup Mixer, Startup Garage Sale). The market persists per city on `GameState` - if you close and reopen, same market. Move to a new city, fresh market. Each option can only be bought once per city (marked "SOLD OUT").

The market is the main way to spend cash, which solves the problem of cash being a meaningless resource. Before markets, cash only mattered when it hit 0 for the grace period. Now it's a strategic resource: save for expensive options or spend early on food when you need it.

Cash validation happens server-side in `GameEngine.resolveMarketPurchase()`. If you can't afford it, the purchase is rejected. No negative cash from overspending.

---

## 14. Testing Strategy

Used:
- JUnit 5
- Mockito

Focus:
- domain logic (clamping, state transitions, multi-location travel)
- action handling (correct resource changes for all 5 actions)
- event system (event application, generation returns valid events)
- turn processing (event chance, action applies without event, turn counter)
- win/loss conditions (health, morale, starvation grace, cash grace, victory priority)
- distance calculation (Haversine and OSRM, with fallback verification)
- score calculation (victory vs defeat, turn efficiency, edge cases)
- data loading (YAML files parse correctly, all categories present)
- all weather adapters (API fallback, mock behavior, demo cycling)

Skipped:
- controller (thin routing layer, tested by playing the game)
- templates (visual, tested by playing)

Tests are focused on logic, not framework internals. 74 tests across 15 test files.

---

## 15. Error Handling

### Principle: handle issues at the boundary

- API fails → fallback to mock/haversine
- invalid action name → caught at controller, returns error (not a 500)
- empty team name / player name → validated and rejected at controller
- database error → warn and continue, don't crash the game
- domain stays clean, values are always valid because the model clamps them

I found the invalid action edge case during a late review. Without the try-catch, sending `POST /api/action?action=GARBAGE` would crash the server with an `IllegalArgumentException`. Now it returns a clean error response. Same pattern for empty names - HTML `required` attributes are client-side only, so I validate server-side too.

No defensive checks inside the game engine. The boundaries already guarantee clean data.

---

## 16. Project Structure

```
silicon-valley-trail/
├── pom.xml
├── mvnw / mvnw.cmd
├── .gitignore
├── README.md
├── docs/
│   ├── DESIGN.md
│   └── TODO.md
└── src/
    ├── main/
    │   ├── java/com/pcunha/svt/
    │   │   ├── SiliconValleyTrailApplication.java
    │   │   ├── domain/
    │   │   │   ├── model/
    │   │   │   │   ├── GameState.java
    │   │   │   │   ├── TeamState.java
    │   │   │   │   ├── ResourceState.java
    │   │   │   │   ├── JourneyState.java
    │   │   │   │   ├── Location.java
    │   │   │   │   ├── GameEvent.java
    │   │   │   │   ├── EventOutcome.java
    │   │   │   │   ├── WeatherSignal.java
    │   │   │   │   ├── ActionInfo.java
    │   │   │   │   └── LeaderboardEntry.java (@Entity)
    │   │   │   ├── port/
    │   │   │   │   ├── WeatherPort.java
    │   │   │   │   ├── DistancePort.java (+ calculateLegDistances default)
    │   │   │   │   └── LeaderboardPort.java
    │   │   │   ├── GameAction.java
    │   │   │   ├── GameMode.java (FAST, ROAD)
    │   │   │   ├── EventCategory.java
    │   │   │   ├── ActionOutcome.java (+ EXHAUSTED)
    │   │   │   ├── WeatherCategory.java
    │   │   │   ├── LossReason.java
    │   │   │   └── StatType.java (HEALTH, ENERGY, MORALE, CASH, FOOD, COMPUTE_CREDIT)
    │   │   ├── application/
    │   │   │   ├── GameEngine.java
    │   │   │   ├── TurnProcessor.java
    │   │   │   ├── ActionHandler.java (data-driven from YAML)
    │   │   │   ├── EventProcessor.java (data-driven from YAML)
    │   │   │   ├── ConditionEvaluator.java
    │   │   │   └── ScoreCalculator.java
    │   │   └── infrastructure/
    │   │       ├── api/
    │   │       │   ├── OpenMeteoAdapter.java
    │   │       │   ├── HaversineDistanceAdapter.java
    │   │       │   ├── OsrmDistanceAdapter.java (batch API, Haversine fallback)
    │   │       │   ├── MockWeatherAdapter.java
    │   │       │   ├── MockDistanceAdapter.java
    │   │       │   ├── DemoWeatherAdapter.java
    │   │       │   └── LeaderboardAdapter.java
    │   │       ├── data/
    │   │       │   └── GameDataLoader.java (YAML deserialization)
    │   │       ├── persistence/
    │   │       │   └── LeaderboardRepository.java
    │   │       └── web/
    │   │           ├── config/GameConfig.java
    │   │           └── controller/GameController.java
    │   └── resources/
    │       ├── application.yml
    │       ├── data/
    │       │   ├── actions.yaml
    │       │   ├── events.yaml
    │       │   ├── markets.yaml
    │       │   └── locations.yaml
    │       ├── static/ (css/, js/, img/)
    │       └── templates/ (start, game, end, leaderboard + fragments)
    └── test/java/com/pcunha/svt/
        ├── domain/ (TeamState, ResourceState, JourneyState)
        ├── application/ (ActionHandler, ConditionEvaluator, EventProcessor, TurnProcessor, ScoreCalculator)
        └── infrastructure/
            ├── api/ (Haversine, OpenMeteo, OSRM, MockWeather, DemoWeather, MockDistance)
            └── data/ (GameDataLoader)
```

Ports live in the domain because the domain defines what it needs. Adapters live in infrastructure because they deal with external stuff. GameConfig wires all dependencies without Spring annotations on domain classes. GameController is the only place that knows about HTTP or Thymeleaf.

---

## 17. Dependencies

- Spring Boot (web, thymeleaf, data-jpa, devtools)
- H2 (default) + PostgreSQL (via env var)
- Jackson + jackson-dataformat-yaml (for YAML game data files)
- spring-dotenv (reads .env file for database config)
- JUnit 5, Mockito
- Lombok (limited use)

DevTools auto-restarts the app when I change code, so I don't have to stop and re-run manually every time. It also disables Thymeleaf caching so template changes show up instantly on refresh.

### Lombok rules

I'll only use Lombok to cut boilerplate, not to hide logic.

**I'll use:**
- `@Getter` on domain models
- `@AllArgsConstructor` / `@RequiredArgsConstructor`
- `@Builder` on objects with many fields like `GameEvent`
- `@ToString`, `@EqualsAndHashCode`

**I'll not use:**
- `@Setter` domain models that have validation (clamping, floors). For example, health must go through changeHealth() to stay between 0-100. Simple flags like gameOver and victory are fine as setters since there's no validation to bypass
- `@Data` because it includes `@Setter`.
- `@SneakyThrows`, `@Log`, `@UtilityClass`. These hide things that should be visible.

No Lombok in the application layer (GameEngine, TurnProcessor, etc). Business logic should be explicit.

---

## 18. Code Practices

- single responsibility per class and method
- no magic numbers, use named constants
- small methods (~20 lines max)
- consistent naming across all layers
- comments only for "why", not "what"

Keeping things simple and readable.

---

## 19. Tradeoffs

| Decision | Benefit | Cost |
|---|---|---|
| Spring Boot + Thymeleaf | Visual game, one project, no JS tooling | Heavier dependency tree |
| Hexagonal architecture | Easy to test, clean API boundary | More files and interfaces |
| 6 stats / 5 actions | Better gameplay, real decisions | More balancing work |
| Category-based events | API integration point, varied gameplay | More complex than flat random |
| 3 APIs (Open-Meteo, OSRM, ORS) | 3 game modes, real data, shows adapter pattern | More adapters, fallback chains |
| H2 default + Postgres via env var | Zero setup for reviewer, real DB for deploy | spring-dotenv dependency |
| YAML data files | Content separate from code, easy to edit | jackson-yaml dependency, lost final fields |
| 4 game modes via speed multiplier | All modes work with no API key, difficulty from mechanics not data | More enum values, slightly complex fallback mapping |
| Pre-computed distances at startup | Instant game creation, no API calls per player | Slower server boot, stale if locations change |
| OSRM → ORS car fallback chain | Road mode works even when OSRM is down | Extra adapter, usedFallback flag complexity |
| Score calculator | Fair per-mode rankings, weighted by difficulty | Formula balancing is subjective |
| Market logic in GameEngine | Controller stays thin, business rules centralized | GameEngine has more methods |
| Market on GameState | Single source of truth, no session cleanup | GameState grows larger |
| MarketResult record | Typed success/error from engine to controller | One more file |

All decisions aim for balance between simplicity and doing things right.

---

## 20. Extension Strategy

The architecture supports these without major changes:

- **Weighted events:** `generateEvent()` already receives GameState and WeatherSignal. Just add category selection logic based on current stats or weather.
- **Multiplayer:** support multiple `TeamState`, one per session
- **CLI:** new adapter that uses Scanner, no engine changes
- **Postgres:** swap H2 URL in `application.yml`, add Flyway migration for `score` index. All JPA code stays the same.
- **More game modes:** add a new `GameMode` enum value and wire a new `DistancePort` adapter. GameEngine picks it automatically.

---

## 21. Implementation Plan

| Day | Focus |
|---|---|
| 1 | Domain models + validation + tests |
| 2 | Core game loop (actions, turns, conditions) |
| 3 | Event system (categories, weights, choices) |
| 4 | API integration + fallback + tests |
| 5 | Web + persistence (Spring Boot, Thymeleaf, H2) |
| 6 | Polish, balance tuning, more tests |
| 7 | README, design doc, screen recording |

The goal is to build the core first, then layer infrastructure on top.

Days 1-4 are pure backend with no Spring Boot. The engine works and is testable before any web code exists. Day 5 adds the web layer and database in one shot since Spring Boot handles both.

---

## Final Notes

The main focus of this project is:
- clear game logic
- proper handling of external APIs
- clean separation of concerns

Everything else is kept simple on purpose to stay within the timeline and avoid unnecessary complexity.
