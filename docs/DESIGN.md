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

4 templates:

| Template | What it shows |
|---|---|
| `start.html` | Team name input, begin game |
| `game.html` | Stats, location, progress, weather, action buttons |
| `event.html` | What happened after the action, choices if any |
| `end.html` | Win or lose screen with final stats |

Plain HTML with Thymeleaf attributes. Minimal CSS for layout and colors. No JavaScript needed for core gameplay.

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
- `GameState`: composes the three above, plus turn number, game-over flag, last event

Each one owns its own rules.

Example:
- `changeHealth(-15)` clamps to 0 internally
- no need for checks everywhere else

This keeps bugs contained and logic easier to reason about.

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

| Condition | What happens | Grace period |
|---|---|---|
| Health hits 0 | Team collapses | Immediate |
| Cash hits 0 | Can't sustain operations | 3 turns to recover |
| Food runs out | Starvation, health starts draining | 2 turns then game over |
| Morale hits 0 | Team quits | Immediate |

Multiple loss conditions force the player to watch more than one number. "I can survive no food for 2 turns if I scavenge now, but my cash is also low..." That kind of tension is what makes it a game.

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

### Decision: Category-based + weighted randomness

Instead of a flat random pool, events are grouped:
- weather
- team
- market
- location
- tech

The weather API affects which category is more likely.

So:
- bad weather to more weather events
- low morale to more team issues

This makes the game feel less random and more reactive.

About 30% of events give the player a choice. Example:

> "A VC offers to fund your startup but wants 40% equity. Accept: +$200 cash, -20 morale. Decline: +10 morale, no cash."

That's what the requirement means by "choices that matter."

---

## 9. API Integration

### Decision: Open-Meteo + Nominatim

- **Weather (Open-Meteo):**
    - no API key needed
    - real weather data for real locations
    - directly affects gameplay (rain to harder travel, heat to morale drop)

- **Distance (Nominatim):**
    - real distances between locations
    - affects resource cost per leg
    - only called once at game start

Both are free and need no setup. Clone the repo and it works.

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
- distance to next (from Nominatim, or hardcoded fallback)
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
- domain logic (clamping, state transitions)
- action handling (correct resource changes)
- event system (weighted selection, event application)
- win/loss conditions (edge cases at thresholds)
- API fallback (mock the API to fail, check game still works)

Skipped:
- controller (thin layer, just routing)
- JPA entities (Hibernate handles that)
- templates (visual, tested by playing the game)

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
├── .env.example
├── .gitignore
├── README.md
├── docs/
│   └── DESIGN.md
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
    │   │   │   │   └── Location.java
    │   │   │   ├── event/
    │   │   │   │   ├── GameEvent.java
    │   │   │   │   ├── EventCategory.java
    │   │   │   │   └── EventOutcome.java
    │   │   │   ├── action/
    │   │   │   │   └── GameAction.java
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
    │   │       │   ├── NominatimAdapter.java
    │   │       │   ├── MockWeatherAdapter.java
    │   │       │   └── MockDistanceAdapter.java
    │   │       ├── persistence/
    │   │       │   ├── entity/
    │   │       │   │   ├── SavedGame.java
    │   │       │   │   ├── TeamSnapshot.java
    │   │       │   │   ├── ResourceSnapshot.java
    │   │       │   │   └── JourneySnapshot.java
    │   │       │   ├── SavedGameRepository.java
    │   │       │   └── JpaPersistenceAdapter.java
    │   │       └── web/
    │   │           └── GameController.java
    │   └── resources/
    │       ├── application.properties
    │       ├── static/
    │       │   └── css/
    │       │       └── game.css
    │       └── templates/
    │           ├── start.html
    │           ├── game.html
    │           ├── event.html
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
                └── OpenMeteoAdapterTest.java
```

Ports live in the domain because the domain defines what it needs. Adapters live in infrastructure because they deal with external stuff. The `web/` package only has one class, `GameController`, which is the only place that knows about HTTP or Thymeleaf.

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
- `@Setter` on domain models. Mutation goes through methods like `changeHealth()` that clamp values. A raw setter would skip that.
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
