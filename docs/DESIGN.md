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
| **Java 21 (LTS)** | Latest LTS, modern features, virtual threads | Slightly newer adoption |

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

### Decision: Open-Meteo + Haversine

- **Weather (Open-Meteo):**
    - no API key needed
    - real weather data for real locations
    - directly affects gameplay (rain to harder travel, heat to morale drop)

- **Distance (Haversine formula):**

I originally planned to use Nominatim (OpenStreetMap) for real distances between locations. But when I started implementing it, I had two options:

**Option A: OpenSource Route Machine/Nominatim API call** to get real driving distances. This means another HTTP call, another fallback, another timeout to handle, and a dependency on an external service. All for a number that the player never sees directly.

**Option B: Haversine formula** to calculate straight-line distance from coordinates. Pure math, no API call, no network, no fallback needed. Always works.

I went with **Option B**. For a game, the exact driving distance between Mountain View and Palo Alto doesn't matter. What matters is that the legs have different lengths so the player has to plan ahead. Haversine gives me that from the real coordinates I already have, with zero infrastructure complexity.

The `DistancePort` interface still exists, so if I ever need real routing distances, I can swap in an API adapter without changing the game logic.

Both APIs need no setup. Clone the repo and it works.

---

### Fallback Strategy

```
try API (3s timeout)
fallback to mock
```

If the API fails, the game still works.

The engine doesn't know or care where the data came from.

---

## 10. Map Design

10 real locations from San Jose to San Francisco:

1. San Jose
2. Santa Clara
3. Sunnyvale
4. Mountain View
5. Palo Alto
6. Menlo Park
7. Redwood City
8. San Carlos
9. South San Francisco
10. San Francisco

Each has:
- real lat/long coordinates (for weather API)
- distance to next (calculated with Haversine from real coordinates)
- possible location-specific events

Real distances matter because they affect resource cost and planning. A long leg costs more, so the player needs to prepare.

---

## 11. Persistence

### Decision: H2 + Spring Data JPA

**Why:**
- zero setup with Spring Boot
- real database behavior
- easy CRUD

Saving/loading is basically one line with `JpaRepository`.

I kept domain models separate from JPA entities to avoid coupling. The domain doesn't know about `@Entity` or `@Id`. The persistence adapter handles the mapping between domain objects and database entities.

### Schema

| Entity | Fields | What it stores |
|---|---|---|
| `SavedGame` | id, teamName, turn, victory, gameOver, createdAt | Root save, one row per game |
| `TeamSnapshot` | health, energy, morale | Team stats at save time |
| `ResourceSnapshot` | cash, food, computeCredits | Resources at save time |
| `JourneySnapshot` | currentLocationIndex, distanceToNext | Progress at save time |

### H2 Config

```properties
# application.yml
spring.datasource.url=jdbc:h2:mem:svt
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

In-memory by default. Data resets on restart, which is fine for a game session. If I want saves to persist, I just change one line to `jdbc:h2:file:./data/svt`.

The H2 console at `/h2-console` is useful for checking the database while developing.

---

## 12. Testing Strategy

Used:
- JUnit 5
- Mockito
- AssertJ

Focus:
- domain logic (clamping, state transitions, multi-location travel)
- action handling (correct resource changes for all 5 actions)
- event system (event application, generation returns valid events)
- turn processing (event chance, action applies without event, turn counter)
- win/loss conditions (health, morale, victory priority)
- distance calculation (Haversine returns valid km)

Skipped:
- controller (thin layer, just routing and session)
- templates (visual, tested by playing the game)
- API fallback tests (OpenMeteo fallback on invalid location, mock adapter validation, demo adapter cycling)

Tests are focused on logic, not framework internals.

---

## 13. Error Handling

### Principle: handle issues at the boundary

- API fails to fallback to mock
- invalid input to blocked at controller level, templates only show valid options
- database error to warn and continue, don't crash the game
- domain stays clean, values are always valid because the model clamps them

No need for defensive checks inside the game engine. The boundaries already guarantee clean data.

---

## 14. Project Structure

```
silicon-valley-trail/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/
│   └── wrapper/
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
    │   │   │   ├── GameState.java
    │   │   │   ├── TeamState.java
    │   │   │   ├── ResourceState.java
    │   │   │   ├── JourneyState.java
    │   │   │   ├── Location.java
    │   │   │   ├── GameAction.java
    │   │   │   ├── GameEvent.java
    │   │   │   ├── EventCategory.java
    │   │   │   ├── EventOutcome.java
    │   │   │   ├── ActionOutcome.java
    │   │   │   ├── LossReason.java
    │   │   │   ├── WeatherSignal.java
    │   │   │   ├── WeatherCategory.java
    │   │   │   └── port/
    │   │   │       ├── WeatherPort.java
    │   │   │       ├── DistancePort.java
    │   │   │       └── PersistencePort.java
    │   │   ├── application/
    │   │   │   ├── GameEngine.java
    │   │   │   ├── TurnProcessor.java
    │   │   │   ├── ActionHandler.java
    │   │   │   ├── EventProcessor.java
    │   │   │   └── ConditionEvaluator.java
    │   │   └── infrastructure/
    │   │       ├── api/
    │   │       │   ├── OpenMeteoAdapter.java
    │   │       │   ├── HaversineDistanceAdapter.java
    │   │       │   ├── MockWeatherAdapter.java
    │   │       │   ├── MockDistanceAdapter.java
    │   │       │   └── DemoWeatherAdapter.java
    │   │       └── web/
    │   │           ├── config/
    │   │           │   └── GameConfig.java
    │   │           └── controller/
    │   │               └── GameController.java
    │   └── resources/
    │       ├── application.yml
    │       ├── static/
    │       │   ├── css/
    │       │   │   ├── base.css
    │       │   │   ├── components.css
    │       │   │   ├── game.css
    │       │   │   ├── game-layout.css
    │       │   │   ├── actions.css
    │       │   │   ├── journey.css
    │       │   │   ├── weather.css
    │       │   │   ├── choice-modal.css
    │       │   │   ├── story.css
    │       │   │   ├── scenes.css
    │       │   │   ├── animations.css
    │       │   │   ├── start.css
    │       │   │   └── end.css
    │       │   ├── js/
    │       │   │   ├── toast.js
    │       │   │   ├── stats.js
    │       │   │   ├── actions.js
    │       │   │   ├── journey.js
    │       │   │   ├── weather.js
    │       │   │   └── animations.js
    │       │   └── img/
    │       │       ├── team-member-1..4.png
    │       │       ├── food-icon.png
    │       │       ├── train.png
    │       │       └── vc-investor-1..2.png
    │       └── templates/
    │           ├── fragments/
    │           │   ├── head.html
    │           │   └── toast.html
    │           ├── start.html
    │           ├── game.html
    │           └── end.html
    └── test/java/com/pcunha/svt/
        ├── domain/
        │   ├── TeamStateTest.java
        │   ├── ResourceStateTest.java
        │   └── JourneyStateTest.java
        ├── application/
        │   ├── ActionHandlerTest.java
        │   ├── EventProcessorTest.java
        │   ├── TurnProcessorTest.java
        │   └── ConditionEvaluatorTest.java
        └── infrastructure/
            └── api/
                ├── HaversineDistanceAdapterTest.java
                ├── MockWeatherAdapterTest.java
                ├── DemoWeatherAdapterTest.java
                ├── OpenMeteoAdapterTest.java
                └── MockDistanceAdapterTest.java
```

Ports live in the domain because the domain defines what it needs. Adapters live in infrastructure because they deal with external stuff. GameConfig wires all dependencies without Spring annotations on domain classes. GameController is the only place that knows about HTTP or Thymeleaf.

---

## 15. Dependencies

- Spring Boot (web, thymeleaf, data-jpa, devtools)
- H2
- JUnit 5, Mockito, AssertJ
- Jackson
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

## 16. Code Practices

- single responsibility per class and method
- no magic numbers, use named constants
- small methods (~20 lines max)
- consistent naming across all layers
- comments only for "why", not "what"

Keeping things simple and readable.

---

## 17. Tradeoffs

| Decision | Benefit | Cost |
|---|---|---|
| Spring Boot + Thymeleaf | Visual game, one project, no JS tooling | Heavier dependency tree |
| Hexagonal architecture | Easy to test, clean API boundary | More files and interfaces |
| 6 stats / 5 actions | Better gameplay, real decisions | More balancing work |
| Category-based events | API integration point, varied gameplay | More complex than flat random |
| Two APIs | Map feels real, weather affects gameplay | Extra adapter code |
| H2 + JPA | Real database, easy CRUD, zero setup | In-memory data lost on restart |
| Open-Meteo | No API key, zero friction | Less data than OpenWeatherMap |

All decisions aim for balance between simplicity and doing things right.

---

## 18. Extension Strategy

I focused on the required features, but the architecture is set up so these can be added later without major changes.

Examples:
- multiplayer: support multiple `TeamState`, one per session
- CLI: new adapter that uses Scanner, no engine changes
- extra APIs: new ports/adapters, plug into event weights
- leaderboard: simple DB query on `SavedGame`

I didn't build these because they're outside scope, but the design supports them.

---

## 19. Implementation Plan

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
