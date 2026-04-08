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

4 templates:

| Template | What it shows |
|---|---|
| `start.html` | Team name input, mode selector (Easy / Medium / Hard / Impossible / Daily), Continue button if a game is in flight |
| `game.html` | Stats, location, progress, weather, action buttons, last turn result and event |
| `end.html` | Win or lose screen with final stats and leaderboard submission |
| `leaderboard.html` | Top 10 ranking, shared between the all-time view and the daily view |

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

### Decision: Composition with value objects, `GameState` as a composition root

`GameState` is deliberately thin, it holds the four inputs the player brings in and composes five focused sub-states that each own a single concern:

**Input fields** (set by the caller, never mutated from outside):
- `teamName`
- `TeamState`: health (0-100), energy (0-100), morale (0-100)
- `ResourceState`: cash, food, compute credits (floor at 0)
- `JourneyState`: locations, distances, current index, distance-to-next

**Composed sub-states** (each with its own behavior):
- `EndingState`: `victory`, `gameOver`, `lossReason`, `isLeaderboardSubmitted` + `markVictory()` / `markLeaderboardSubmitted()`, semantic one-way transitions, no external setters for the victory/submitted flags
- `ResourceGraceState`: `turnWithoutCash`, `turnWithoutFood` + increment/reset methods for the grace-period loss counters
- `MarketState`: `currentMarketEvent`, `marketCityIndex`, `marketPurchased` + `resetMarket()` / `addMarketPurchase()`, survives across city visits
- `ConfigState`: `gameMode`, `requestedGameMode`, `usedFallbackDistances`, set once at game creation
- `ProgressState`: `turn` + `nextTurn()`, `currentWeather` / `currentWeatherTemperature` (cached from the Open-Meteo signal so the UI can render it on cold page loads), and `pendingEvent` (the one-event-at-a-time choice that survives the POST-Redirect-GET flow until the player resolves it)

`GameState` itself has zero methods and zero `@Setter` annotations, every mutation happens on a sub-state via a named method, so every call site in the codebase self-documents which subsystem it's touching. Callers navigate explicitly (e.g. `gameState.getEndingState().isGameOver()` instead of `gameState.isGameOver()`). Slightly more verbose at the call site, dramatically clearer about intent.

**Why sub-states, not a flat god object?** Earlier versions had `GameState` with ~20 fields mixing core inputs, runtime counters, market state, and end-game flags. Every subsystem was reaching into the same mutable bag, which made it hard to reason about who owned what. Extracting these into cohesive sub-states forced each concern to name itself, and the `@RestControllerAdvice` + `ActionResponse` work in the web layer could then rely on a predictable JSON shape.

**Per-turn data does NOT live on state.** `TurnResult` is a separate DTO returned by `TurnProcessor.processTurn()`, `gameAction`, `actionOutcome`, `gameEvent`, and the weather values observed during that specific turn. On the MVC flow, it rides through Spring's `RedirectAttributes.addFlashAttribute` across the POST-Redirect-GET boundary. On the REST flow, it's wrapped in a typed `ActionResponse(state, turnResult)` envelope so JS clients can reconcile the persistent state with the per-action delta. The `TurnResult` is never cached on `GameState`, that was an earlier design that conflated state with response; the separation happened when `ProgressState` shed its `lastTurnResult` field.

Each sub-state owns its own validation rules. Example: `TeamState.changeHealth(-15)` clamps to 0 internally, so no caller needs to check bounds. Bugs stay contained.

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

The grace period counters (`turnWithoutFood`, `turnWithoutCash`) live on `ResourceGraceState` and reset when the resource is recovered. The `LossReason` enum is set by `ConditionEvaluator` on `EndingState` and displayed on the end screen.

---

## 7. Actions

I implemented 5 actions, all data-driven from `actions.yml`. Every action has guaranteed effects plus an optional set of mutually exclusive outcome rolls. Exactly one roll fires per turn, picked at random.

| Action | Guaranteed | Outcome rolls (one fires) |
|---|---|---|
| **Travel** | +distance, -10 energy, -1 food, -2 compute | -5 morale OR -5 health |
| **Rest** | +8 health, +15 energy, +8 morale, -1 food | none, rest is safe |
| **Scavenge** | -10 energy, -3 health | +3 food (clean) OR (+15 cash, -3 morale) (dirty) |
| **Hackathon** | +15 compute, -12 energy, -1 food | jackpot (+15 cash, +1 food) OR wreck (-8 morale, -8 health) |
| **Pitch VCs** | -5 energy, -3 compute, -2 morale | +50 cash OR (-8 morale, -3 health) |

More than 3 gives better gameplay without adding much complexity. Each action has a clear role so nothing overlaps.

Travel, Hackathon, and Pitch VCs are the gambling actions. Travel rolls between a small morale hit or a small health hit every turn, so a long Hard or Impossible run is a war of attrition you cannot fully predict but never feels brutal in any single turn. Hackathon is the high-variance compute farm: jackpot pays cash and food on top of the compute, wreck takes morale and health. Pitch VCs is the cash gamble: hit the deal for +50 cash, miss and pay morale and a sliver of health.

Scavenge is an interesting middle case. Both rolls are gains (food or cash), but the cash branch comes with a morale hit because the team had to do something they would rather not talk about. Rest is the only fully safe action.

The balance went through several rounds to land here. Earlier versions had Travel doing -15 energy and rolling -10 morale or -10 health, which made the game a constant scramble for recovery. The 2 travel + 1 rest cycle is now the natural rhythm: 6 km of progress every 3 turns with stats roughly stable, plus food and compute drains pushing you into markets and Hackathon for refills.

Starting resources in `GameEngine.createNewGame` are tuned to match: cash 100, food 8, compute 8. The food gives ~8 turns of breathing room before food hunting becomes urgent. The compute keeps the first 4 travels at full speed (compute -2 per travel) before the speed penalty kicks in.

---

## 8. Event System

### Decision: Arrival-triggered events with weather-weighted selection

Events are grouped into 5 categories (weather, team, market, location, tech) with 3+ events each.

**When events fire.** Events trigger deterministically on arrival at each intermediate city, not on a per-turn probability. This matches the spec ("an event should happen at each location after movement") and is also better game feel, players associate each city with a story moment rather than random button-click variance. Travel turns that don't cross a city boundary don't trigger events; rest/hackathon/scavenge/pitch turns never trigger events because they don't move you. Arriving at the final destination short-circuits to victory without firing an event, victory is the story beat.

**What event fires.** The selection is weather-weighted via the Open-Meteo signal, which is the spec's "events conditional on API data":

- **Rough weather** (rainy, stormy, heatwave): 70% chance the event is drawn from the `WEATHER` category pool ("Heat Wave", "Fog Delay", "Incoming Storm"), 30% chance it's drawn from the full pool.
- **Clear weather**: 15% chance of a weather-category event ("Clear Skies"), 85% chance from the full pool.

Both constants live in `EventProcessor` as `WEATHER_BIAS_ROUGH` and `WEATHER_BIAS_CLEAR`. This means the live API response materially shifts what the player encounters, rainy days feel different from sunny days, and "Clear Skies" isn't dead content sitting in the YAML.

**Why weighted, not hard-filtered.** A hard filter ("rough weather always means a weather event") would be simpler but makes bad weather predictable. The 70/30 split keeps variety while still making the world feel responsive. On clear days the 15% chance lets good-weather events surface occasionally without dominating.

**Choice events.** 5 events use the `EventOutcome` model to present the player with two options that have different stat tradeoffs (Incoming Storm, Engineer Wants to Quit, etc.). The choice modal pauses the game until the player decides, and the pending event lives on `ProgressState` so it survives across requests until resolved.

---

## 9. API Integration

### Decision: Open-Meteo + Haversine + OSRM + OpenRouteService

**Weather (Open-Meteo):**
- no API key needed
- real weather data for real locations
- directly affects gameplay (rain to harder travel, heat to morale drop)
- weather effects only apply when traveling, not when resting. I added this restriction because without it, players could exploit clear weather by spamming REST to get free health and energy. Now resting is "indoors" and weather only hits you on the road.

**No keys required, by design.**

This was a deliberate choice from day one. The reviewer should be able to clone, run, and play without signing up for anything. Every API in the stack is free and keyless:

- Open-Meteo for weather
- OSRM public server for driving distances
- Haversine for straight-line distances (pure math, always works)

OpenRouteService is the only optional one. It needs a free key but you only need it if OSRM is acting up. The fallback chain handles its absence gracefully.

I considered using paid or key-required APIs (Google Maps Directions, Mapbox) and rejected them. The friction of "go sign up, paste a key, then run" would have been the first thing a reviewer hit, and that is a bad first impression for a take-home.

**Distance (3 APIs, 4 game modes):**

I started with just Haversine for straight-line distances. Then added OSRM for driving distances. Then realized I could make each API a game mode, so I added OpenRouteService for walking distances.

| Mode | API | Key needed | Difficulty |
|---|---|---|---|
| Easy | Haversine (math) | No | Easy (~65 km journey) |
| Medium | OSRM driving with ORS car fallback | No (ORS key optional) | Medium (~106 km journey) |
| Hard | Haversine, slow speed | No | Hard (longer because of speed) |
| Impossible | OSRM driving, slow speed | No (ORS key optional) | Hardest |

The `DistancePort` interface made this painless. Each mode is just a different adapter. GameEngine picks the right one based on the player's selection. Zero changes to game logic.

Travel distance per turn is 3 km on Easy and Medium, 1.2 km on Hard and Impossible (the slow modes apply a 0.4 speed multiplier). Defined in `actions.yml` and `GameMode`. Lower than the original 5 km because the journey was finishing too fast for the morale and health stats to actually matter.

**OSRM is the weak link.** The public OSRM server is free but slow and rate-limited. In practice it succeeds about 80% of the time for the batch of 15 waypoints. When it fails, the fallback chain picks up. For reviewer experience I recommend either using the live demo (cached distances are warm) or dropping a free `ORS_API_KEY` in `.env` so OpenRouteService takes over before Haversine kicks in.

**Pre-computed distances:** Originally I calculated distances when the player clicked "Start." That meant API calls blocked game creation. OSRM's public server is slow and unreliable, so Road mode could hang for 15+ seconds.

I moved the computation to application startup. All distances are cached once when the server boots. Game creation is instant - just reads from the cache. If an API was down at startup, the player can retry from a modal without restarting the server.

**Batch requests:** Both OSRM and ORS support sending all 15 waypoints in a single request. I used this instead of 14 separate calls, which would have been painfully slow.

**Locale bug:** `String.format("%f")` uses locale-specific decimal separators. On non-US systems, coordinates get commas instead of dots, breaking the API URL. Fixed with `Locale.US` everywhere I format coordinates.

**Weather caching:** Weather is also pre-loaded at startup for all 15 cities and cached for 1 hour. During gameplay, `loadWeather()` reads from the in-memory cache, no API calls mid-game. After the TTL expires, the next request for that city refreshes the cache.

All 4 game modes work with no API key. Walking modes use the same road distances as their normal-speed equivalents but with a 0.4x speed multiplier, difficulty comes from game mechanics, not from a different API.

---

### Fallback Strategy

Each adapter follows the same pattern: try the API, fall back on failure.

- `OpenMeteoAdapter` to `MockWeatherAdapter`
- `OsrmDistanceAdapter` to `OpenRouteServiceAdapter` (car profile) to `HaversineDistanceAdapter`
- `OpenRouteServiceAdapter` to `HaversineDistanceAdapter`

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

### Decision: H2 + Spring Data JPA, three entities

I originally planned save/load with multiple snapshot entities. The game is short enough that mid-game save is not needed, so I pivoted to a leaderboard plus a small session-resume layer. Three entities cover everything:

| Entity | What it stores |
|---|---|
| `LeaderboardEntry` | One row per finished game. Player, team, mode, outcome, location, turns, raw score, weighted score, daily flag |
| `Room` | Container for one or more sessions. Solo rooms are private. Daily rooms key on `(mode, date)` so all players today share one RNG seed |
| `GameSession` | One player's slot in a Room. Holds the serialized `GameState`, the `playerToken` cookie identity, and a JPA `@Version` for optimistic locking |

`LeaderboardEntry` is a pure JPA row. The mapping from `GameState` lives in `LeaderboardService.buildEntry()`, so the entity has no factory method and `GameState` has no projection method. Persistence stays at the edge.

### Score multiplier and the unified leaderboard

`GameMode` carries a `scoreMultiplier`: EASY 1.0, MEDIUM 1.3, HARD 1.7, IMPOSSIBLE 2.2. `buildEntry()` computes `weightedScore = round(score * multiplier)` at submission time and stores both numbers. The leaderboard ranks on weighted; raw is kept for display and recompute.

The old design split rankings four ways, one per mode. With five entries each, the "competing against people" feeling collapsed. A single weighted leaderboard with a mode badge per row tells one story instead of four, and lets a great Impossible run beat a mediocre Easy run on the same chart. Speedrunning leaderboards handle category weighting the same way.

The curve is super-linear because death risk grows non-linearly with difficulty. Linear weights would over-reward Impossible. Sub-linear weights would never make Impossible worth playing. Storing both raw and weighted means I can retune the curve later without wiping history, and the UI shows the math (`raw x mult = score`) so players understand the number.

### ScoreInputs vs Scoring (two records, on purpose)

`ScoreInputs` (in `domain.model`) is per-run data: victory, turns, journey progress, stats, resources. `Scoring` (in `application`) is formula parameters: bonuses, weights, caps. They live at different layers and change for different reasons, so collapsing them would either pull formula constants into the domain or push per-run state into the application layer.

`ScoreCalculator` is a `@Component` that takes `Scoring` via DI. It exposes three overloads (`calculate(GameState)`, `calculate(LeaderboardEntry)`, `calculate(ScoreInputs)`) so both live state and stored rows can be scored. `Scoring` is loaded from `resources/data/scoring.yaml`, same pattern as `tunables.yaml`. Rebalancing is an edit, not a recompile.

### Mode rename

Modes used to be FAST / ROAD / WALKING_FAST / WALKING_ROAD. Those names described which API powered them. They are now EASY / MEDIUM / HARD / IMPOSSIBLE. Same internal pairing of speed and distance source, player-facing labels instead of implementation labels. The adapter classes keep their implementation-flavored names because that is the right naming layer for them.

### Cross-device resume with Room and GameSession

The HTTP session approach lost state on browser quit and server restart. Now every player gets a `svt_player` cookie (UUID, 30-day max-age, HttpOnly). On every request the controller reads the token, looks up the active GameSession via `RoomService`, deserializes the `GameState`, mutates it via `GameEngine`, and persists back.

The home page shows a Continue button next to Launch when an active session exists. For cross-device handoff there is a `/resume/{token}` endpoint. The collapsible "Resume on another device" block on the home page exposes the URL with click-to-copy. Open it on another browser and you are dropped straight into the game.

Why a Room and a GameSession instead of just a GameSession? The Room is the join point that makes daily mode (and future multiplayer) cheap. Without Room, daily would need its own bespoke shared-seed mechanism bolted onto sessions. With Room, daily is `RoomType.DAILY` plus a `dateBucket` lookup. Multiplayer would be `RoomType.MULTI` plus a turn scheduler, no domain changes.

There is no Player table. Adding one buys nothing without an account system. The cookie is the identity.

### Serialization into a TEXT column

`GameSession.gameStateData` is a `String` column with `@Column(columnDefinition = "TEXT")`. No `@Lob`, no length to justify. The serialized `GameState` is encoded as Base64 so the column holds it as plain text.

I went with Java native serialization instead of Jackson JSON because `GameState` has 9 final fields. Jackson would force either dropping `final` (invasive) or adding a 9-arg `@JsonCreator` constructor (also invasive). Java native needs only `implements Serializable` on each leaf class. 12 classes, one line each, no behavior change. The cost is that the stored data is not human-readable in the DB and is brittle to class refactors. Both costs are acceptable here: H2 in-memory wipes on every restart, and class refactors do not happen mid-game.

`TEXT` is the cleanest annotation in the JPA toolbox. Both H2 and Postgres support it natively (Postgres uses TOAST to handle large values transparently). One keyword, one sentence to explain, no LOB semantics. Not portable to Oracle, but Oracle is not in scope.

### Daily mode

Daily runs are Room instances with `type=DAILY` and a `dateBucket` of today's `LocalDate`. The seed is derived from `(mode, date)` so all players today see the same event sequence and scavenge rolls. Each player has their own GameSession (their own team and decisions), only the world is shared.

Daily always uses `GameMode.EASY`. One fixed difficulty per day keeps the bracket meaningful. The UI exposes Daily as a fifth button in the mode selector with the magic value `gameMode=DAILY`. One Launch button submits whichever the player picked.

The leaderboard splits into two views sharing one template. `/leaderboard` shows the all-time top 10 (filtered to `dailyRun=false`). `/leaderboard/daily` shows today's top 10 from DAILY rooms. The two queries are strictly disjoint, so daily players never appear on the all-time board and vice versa. Mixing them would defeat the daily bracket.

There is a small race window I deliberately did not fix. If two players hit `createOrJoinDailyGame` in the same millisecond, both will see `findDailyRoom` return empty and both will create a fresh room. The seed is deterministic per `(mode, date)`, so both rooms have the same RNG and the player experience is identical. The only artifact is two Room rows in the database. A unique constraint would force retry logic for a failure that is invisible to players. Documented, not fixed.

### Optimistic locking

Two tabs (or two devices) clicking simultaneously would race. `@Version` on `GameSession` turns the silent last-write-wins into an `OptimisticLockingFailureException`. The action endpoint catches it and redirects to `/game` so the player reloads the latest state instead of seeing a 500.

### Security model

The cookie + `/resume/{token}` URL is the entire auth model. The token is a UUID v4, 122 random bits, unguessable in practice. Anyone with the URL can resume the game, that is the point. Treat it like a password reset link or a magic email link. Shareable to your other device, not posted publicly.

The endpoint validates the token is a valid UUID format before issuing the cookie, so typos and scrapers get redirected home instead of polluting the cookie store. Since the game stores no PII, the worst case if a token leaks is "someone plays your game for you", embarrassing but not damaging.

There is no password recovery because there is no password. Clear your cookies and lose the URL, and the run is gone. The cost of an account-free identity.

### Server-side name validation

`LeaderboardService.submitResult()` rejects null, blank, or 11+ character player names, and refuses double submits on the same finished game. The HTML input has `maxlength="10"` for client UX, the server enforces the same bound because HTML attributes are advisory and a `curl` post would bypass them.

### State location

Earlier I had market state and leaderboard submission as HTTP session attributes, which meant cleanup code in `POST /start` to clear stale data. Moving everything onto GameState was cleaner: new game = new object = everything resets automatically.

### Database config

`${DB_URL:jdbc:h2:mem:svt}` in `application.yml`. Default is H2 in-memory, so reviewers run the app with zero setup. To switch to Postgres, set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`. `spring-dotenv` reads `.env` if present. `docker-compose.yml` starts Postgres 16 with the default credentials so `docker compose up -d` is enough.

I considered Spring profiles but decided against it. One `application.yml` with env var defaults is simpler. No flags to remember, no profile switching.

For production at scale I would add Flyway for schema migrations and indexes on `weightedScore` and `playerToken`. With `ddl-auto: update` and a handful of rows, not needed here.

### OSRM fallback and fair rankings

When OSRM is unavailable the adapter falls back to Haversine and sets `usedFallback=true` on the `DistanceResult`. The game mode then automatically downgrades from a road mode to its straight-line equivalent so the score does not compete unfairly against players who got real driving distances. The player sees a toast on game load explaining what happened.

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
| `tunables.yaml` | Game balance knobs: food/cash grace turns, weather stat deltas per category |

`GameDataLoader` handles deserialization. Jackson + `jackson-dataformat-yaml` does the heavy lifting. The domain models needed `@NoArgsConstructor` for Jackson, which meant dropping `final` fields. Worth it for the cleaner separation.

**Balance lives in YAML, not Java constants.** An earlier iteration kept weather effects and grace-turn limits as `public static final` constants on a `Tunables` holder class. That worked but meant rebalancing required a recompile. Moving the values to `tunables.yaml` and loading them via `GameDataLoader.loadTunables()` into a `Tunables` record (with a nested `WeatherEffects` sub-record and a reusable `StatDelta` value type in `domain.model`) puts every balance knob in one editable place. `TurnProcessor` and `ConditionEvaluator` take the record via constructor injection, so the processors stay pure logic and the knobs stay data.

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

Tests are focused on logic, not framework internals. 89 tests across 16 test files.

---

## 15. Error Handling

### Principle: handle issues at the boundary

- API fails, fall back to mock or Haversine
- invalid action name, caught at the controller, returns an error not a 500
- empty team name or player name, validated and rejected at the controller
- database error, warn and continue, don't crash the game
- domain stays clean, values are always valid because the model clamps them

I found the invalid action edge case during a late review. Without the try-catch, sending `POST /api/action?action=GARBAGE` would crash the server with an `IllegalArgumentException`. Now it returns a clean error response. Same pattern for empty names, HTML `required` attributes are client-side only, so I validate server-side too.

No defensive checks inside the game engine. The boundaries already guarantee clean data. `@RestControllerAdvice` in `GlobalExceptionHandler` catches `NoGameInSessionException` centrally and translates it to a `404` with an `ApiError` body, so the REST endpoints don't each repeat the same null-guard.

### Network failures and rate limits

Every HTTP adapter uses a **3-second connect timeout** and a **3-second request timeout**. If an upstream API hangs, the request aborts and the fallback chain fires, nothing blocks the game thread for more than a few seconds.

Rate limits are handled **proactively via caching, not reactively via backoff**:

- **Weather:** `OpenMeteoAdapter` pre-loads all 15 cities once at startup and caches each result for 1 hour. During gameplay, `loadWeather()` reads from the in-memory cache, zero API calls per turn. After the TTL expires, the next request for that city refreshes the cache. For a 30-minute session, total weather API calls are 15 (at boot) plus at most 15 (if the TTL rolls over). For the Open-Meteo free tier that's well under any rate limit.
- **Distance:** OSRM and OpenRouteService are called once each at startup with a batch request containing all 15 waypoints, not 14 separate per-leg calls. Results are cached in memory for the process lifetime. Subsequent games read from the cache. In-game there are zero distance API calls ever.
- **Leaderboard:** In-process database (H2 by default), no external API. N/A.

The result: for a reviewer playing a full game, the app hits Open-Meteo ~15 times (once per location, cached), OSRM ~1 time (batched), and that's it. Rate limits are a non-issue by construction rather than by retry logic.

### No personal user information collected

The spec requires "no collection of any personal user information." The app stores exactly two free-text fields: `teamName` (entered on the start screen, kept on the persisted GameSession) and `playerName` (entered on the end screen, written to the leaderboard). Both are self-chosen nicknames, there is no email, address, IP logging, or analytics instrumentation. H2 runs in-memory by default so even the leaderboard disappears on process restart unless the reviewer swaps to Postgres. `.env` holds configuration only, never user data.

The `svt_player` cookie carries a randomly generated UUID with a 30-day lifetime. This is not PII. It is an opaque identifier with no link to a real person, functionally equivalent to the `JSESSIONID` Spring would set anyway, just with a longer lifetime so the player can resume across browser quits. No fingerprint, no IP, no behavioral tracking. Clearing cookies is the player's log out path. Under GDPR, identifiers used purely for strictly necessary session continuation are exempt from consent, and that is the only thing this token does.

---

## 16. Project Structure

```
silicon-valley-trail/
├── pom.xml
├── mvnw / mvnw.cmd
├── docker-compose.yml
├── .env.example
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
    │   │   │   │   ├── GameState.java                  (composition root)
    │   │   │   │   ├── TeamState.java                  (health, energy, morale)
    │   │   │   │   ├── ResourceState.java              (cash, food, compute)
    │   │   │   │   ├── JourneyState.java               (locations, distances, progress)
    │   │   │   │   ├── EndingState.java                (victory, gameOver, lossReason)
    │   │   │   │   ├── ResourceGraceState.java         (food/cash death clocks)
    │   │   │   │   ├── MarketState.java                (current market + purchases)
    │   │   │   │   ├── ConfigState.java                (game mode, fallback flag)
    │   │   │   │   ├── ProgressState.java              (turn, weather, pending event)
    │   │   │   │   ├── Location.java
    │   │   │   │   ├── GameEvent.java
    │   │   │   │   ├── EventOutcome.java
    │   │   │   │   ├── WeatherSignal.java
    │   │   │   │   ├── ActionInfo.java
    │   │   │   │   ├── ActionResponse.java             (REST envelope)
    │   │   │   │   ├── ApiError.java
    │   │   │   │   ├── DistanceResult.java
    │   │   │   │   ├── MarketResult.java
    │   │   │   │   ├── ScoreInputs.java
    │   │   │   │   ├── StatDelta.java
    │   │   │   │   ├── SubmissionResult.java
    │   │   │   │   ├── TurnResult.java
    │   │   │   │   ├── LeaderboardEntry.java           (@Entity)
    │   │   │   │   ├── Room.java                       (@Entity, SOLO or DAILY)
    │   │   │   │   └── GameSession.java                (@Entity, holds serialized GameState)
    │   │   │   ├── port/
    │   │   │   │   ├── WeatherPort.java
    │   │   │   │   ├── DistancePort.java               (with calculateLegDistances default)
    │   │   │   │   ├── LeaderboardPort.java
    │   │   │   │   ├── RoomPort.java
    │   │   │   │   └── GameSessionPort.java
    │   │   │   ├── GameAction.java
    │   │   │   ├── GameMode.java                       (EASY, MEDIUM, HARD, IMPOSSIBLE)
    │   │   │   ├── RoomType.java                       (SOLO, DAILY)
    │   │   │   ├── RoomStatus.java                     (ACTIVE, COMPLETED)
    │   │   │   ├── EventCategory.java
    │   │   │   ├── ActionOutcome.java
    │   │   │   ├── WeatherCategory.java
    │   │   │   ├── LossReason.java
    │   │   │   └── StatType.java
    │   │   ├── application/
    │   │   │   ├── GameEngine.java
    │   │   │   ├── TurnProcessor.java
    │   │   │   ├── ActionHandler.java                  (data-driven from YAML)
    │   │   │   ├── EventProcessor.java                 (data-driven from YAML)
    │   │   │   ├── ConditionEvaluator.java
    │   │   │   ├── ScoreCalculator.java
    │   │   │   ├── LeaderboardService.java
    │   │   │   ├── RoomService.java                    (session lifecycle + serialization)
    │   │   │   ├── Scoring.java                        (loaded from scoring.yaml)
    │   │   │   └── Tunables.java                       (loaded from tunables.yaml)
    │   │   └── infrastructure/
    │   │       ├── api/
    │   │       │   ├── OpenMeteoAdapter.java
    │   │       │   ├── HaversineDistanceAdapter.java
    │   │       │   ├── OsrmDistanceAdapter.java
    │   │       │   ├── OpenRouteServiceAdapter.java
    │   │       │   ├── MockWeatherAdapter.java
    │   │       │   ├── MockDistanceAdapter.java
    │   │       │   ├── DemoWeatherAdapter.java
    │   │       │   └── LeaderboardAdapter.java
    │   │       ├── data/
    │   │       │   └── GameDataLoader.java
    │   │       ├── persistence/
    │   │       │   ├── LeaderboardRepository.java
    │   │       │   ├── RoomRepository.java
    │   │       │   ├── RoomAdapter.java
    │   │       │   ├── GameSessionRepository.java
    │   │       │   └── GameSessionAdapter.java
    │   │       └── web/
    │   │           ├── PlayerCookies.java              (read or issue svt_player cookie)
    │   │           ├── config/GameConfig.java
    │   │           ├── controller/GameMvcController.java
    │   │           ├── controller/GameRestController.java
    │   │           └── exception/
    │   │               ├── GlobalExceptionHandler.java
    │   │               └── NoGameInSessionException.java
    │   └── resources/
    │       ├── application.yml
    │       ├── data/
    │       │   ├── actions.yaml
    │       │   ├── events.yaml
    │       │   ├── markets.yaml
    │       │   ├── locations.yaml
    │       │   ├── tunables.yaml
    │       │   └── scoring.yaml
    │       ├── static/    (css, js, img)
    │       └── templates/ (start, game, end, leaderboard + fragments)
    └── test/java/com/pcunha/svt/
        ├── domain/         (TeamState, ResourceState, JourneyState)
        ├── application/    (ActionHandler, ConditionEvaluator, EventProcessor,
        │                    TurnProcessor, ScoreCalculator, LeaderboardService)
        └── infrastructure/
            ├── api/        (Haversine, OpenMeteo, OSRM, MockWeather, DemoWeather, MockDistance)
            └── data/       (GameDataLoader)
```

Ports live in the domain because the domain defines what it needs. Adapters live in infrastructure because they deal with external stuff. `GameConfig` wires all dependencies without Spring annotations on domain classes. The two controllers (`GameMvcController` for Thymeleaf pages, `GameRestController` for REST endpoints under `/api`) are the only places that know about HTTP.

---

## 17. Dependencies

- Spring Boot (web, thymeleaf, data-jpa)
- H2 (default) + PostgreSQL (via env var)
- Jackson + jackson-dataformat-yaml (for YAML game data files)
- spring-dotenv (reads .env file for database config)
- JUnit 5, Mockito
- Lombok (limited use)

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
| OSRM to ORS car fallback chain | Road mode works even when OSRM is down | Extra adapter, usedFallback flag complexity |
| Score calculator | Fair per-mode rankings, weighted by difficulty | Formula balancing is subjective |
| Scoring params in `scoring.yaml` | Rebalance without recompile, consistent with `tunables.yaml` | One more YAML file and bean wiring |
| Store raw inputs on `LeaderboardEntry` | Recompute scores against new formula without wiping table | 3 extra columns duplicating score-implied data |
| `ScoreInputs` separate from `Scoring` | Honest layering, domain data vs application config | Two records instead of one |
| Outcome + location columns on leaderboard | Tells a story per row, not just a number | Tighter table layout |
| Filler rows to 10 slots | No layout shift on first submission | Template needs a sequence guard |
| Rename modes to Easy/Medium/Hard/Impossible | Player-facing labels match difficulty, not API implementation | Breaking change for any persisted gameMode rows |
| Drop `GameMode.description` field | One less unused string, the label is self-explanatory | Lose a place to put per-mode flavor text |
| `scoreMultiplier` curve 1.0/1.3/1.7/2.2 | Harder modes are worth picking, speedrunning-style category weighting | Multipliers are subjective and need playtest tuning |
| Single weighted leaderboard | One ranking that means something across modes | Loses per-mode "best in class" visibility |
| Store both raw and weighted score | Retune multipliers without wiping history, UI shows the math | One extra int column |
| Drop `getTopScores(GameMode)` from port | Remove dead API surface once unified leaderboard ships | Per-mode queries need re-adding if future features want them |
| Room + GameSession + cookie identity | Cross-device resume, foundation for daily mode and future multiplayer | Two new entities, two new ports, a new service |
| Java native serialization to a TEXT column | One-line Serializable per class, no Jackson config, no `@Lob` | Not human-readable in the DB, brittle to class refactors |
| `@Column(columnDefinition = "TEXT")` | No length to justify, no LOB semantics, works on H2 and Postgres | Not portable to Oracle (uses CLOB) |
| `playerToken` cookie + `/resume/{token}` | Cross-device works without accounts or PII | Lose the cookie and the URL, lose the run |
| `@Version` optimistic locking on GameSession | Two-tab race surfaces as a visible exception, not silent data loss | One extra column, one catch block |
| Daily room keyed on (mode, dateBucket) | Shared seed across all players today, reproducible runs | Daily leaderboard needs a flag to separate from solo |
| Two leaderboard views | Daily competition stays its own bracket, all-time stays meaningful | One more controller endpoint, one new query method |
| Market logic in GameEngine | Controller stays thin, business rules centralized | GameEngine has more methods |
| Market on GameState | Single source of truth, no session cleanup | GameState grows larger |
| MarketResult record | Typed success/error from engine to controller | One more file |

All decisions aim for balance between simplicity and doing things right.

---

## 20. Extension Strategy

The architecture supports these without major changes:

- Weighted events: `generateEvent()` already receives GameState and WeatherSignal. Add category selection logic based on current stats or weather.
- Real multiplayer rooms: the `Room` + `GameSession` model already supports it. What is left is purely runtime. Add a `RoomType.MULTI` enum value, a `@Scheduled` task that scans `Room.turnDeadline < now()` and force-resolves unsubmitted turns, a polling endpoint for clients (`GET /room/{id}/state`), a lobby UI, and `currentTurn` and `turnDeadline` fields on `Room`. The domain, the engine, the scoring, and the existing single-player flow do not change. Multiplayer is additive, not a refactor.
- CLI: new adapter that uses Scanner, no engine changes.
- Postgres: swap the H2 URL in `application.yml`, add Flyway migrations for `weightedScore` and `playerToken` indexes. All JPA code stays the same.
- More game modes: add a new `GameMode` enum value with its multiplier and wire a new `DistancePort` adapter. GameEngine picks it automatically.

### If I had more time

- Integration tests for `RoomService`. Round-trip serialization, daily-room race coverage, optimistic-lock failure scenarios. The native Java serialization is the brittlest part of the system (refactoring `GameState` would silently break stored sessions on Postgres) and deserves a regression test pinning the contract.
- Background cleanup job. A `@Scheduled` task to delete completed `GameSession` rows older than 30 days and orphaned `Room` rows with no active sessions. Irrelevant on H2 in-memory, necessary for any Postgres deployment that runs longer than a few weeks.
- `OptimisticLockingFailureException` in the REST controller too. The MVC controller catches it on the action endpoint but the REST endpoints do not. Same fix, same shape, did not get to it before submission.
- Real multiplayer rooms. The data model is ready (see above). Building it would be a focused weekend project, not a refactor.
- Flyway migrations. Currently `ddl-auto: update` handles schema. For production, Flyway gives versioned migrations, rollback, and the ability to backfill columns deliberately.
- Resume URL polish. The current "open this URL on another device" flow works but is unfussy. A QR code or a 6-character resume code with a separate code-to-token lookup would feel more product-y. Skipped because it is strictly more complex code for the same outcome.
- CSRF protection. The project does not use Spring Security so Thymeleaf forms post without CSRF tokens. For a take-home this is fine. For a public deployment it would be the next layer to add.

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

## 22. Extras

Spec required: 3 actions, 3 stats, 10 locations, events with choices, 1 public API. Built on top of that:

**5 game modes instead of 1.** Each distance API became a difficulty tier so the API integration felt like a player choice rather than backend wiring. Daily was the last addition, almost free once Room existed.

**6 stats instead of 3.** Three stats made the game too predictable. With six (health, energy, morale, cash, food, compute credits) every action becomes a real tradeoff. After the morale and health rebalance, Travel costs morale and a sliver of health every turn, so spamming it is no longer free.

**15 locations instead of 10.** Started at the minimum, added 5 more during balance tuning. More cities means more market opportunities and more arrival events.

**3 distance APIs in a fallback chain.** OSRM, OpenRouteService car profile, Haversine. The chain passes through a `usedFallback` flag so if the player ends up on estimated distances, the leaderboard automatically downgrades the mode for fair ranking.

**Live weather biases the event pool, not just stat deltas.** The Open-Meteo signal shifts which events are likely to fire, on top of applying its own per-category stat changes.

**20+ events across 5 categories with 7 player-choice events.** YAML-driven event system with weighted category selection and multi-option choices.

**Cross-device resume.** `svt_player` UUID cookie with 30-day persistence plus a `/resume/{token}` endpoint. The home page exposes the URL with click-to-copy. No accounts, no email, no PII.

**Two leaderboard views weighted by mode multiplier.** All-time top 10 plus a separate today's daily top 10. Mode multiplier (Easy 1.0 to Impossible 2.2) lets a great Impossible run beat a great Easy run on the unified ranking. The two views are kept disjoint so daily players never bleed into the all-time bracket.

**Optimistic locking on game sessions.** `@Version` on `GameSession`. Two tabs or two devices clicking at the same time surface as a visible exception that the action endpoint catches and redirects to `/game`.

**Persistence layer with Room and GameSession.** Foundation for future multiplayer. Adding it would be additive (one new RoomType, a turn scheduler, a polling endpoint), not a refactor.

**Server-side validation on player names.** 10 character cap, non-blank, no double submit. HTML attributes are advisory, a `curl` post would bypass them.

**Hexagonal architecture with ports in domain and adapters in infrastructure.** Swapping an API is a one-file change.

**Game content fully in YAML.** `actions.yml`, `events.yml`, `markets.yml`, `locations.yml`, `tunables.yml`, `scoring.yaml`. Rebalancing is an edit, not a recompile. The morale and health rebalance in the latest pass was YAML only, no code changes.

**89 tests across 16 files** covering domain, application, and infrastructure layers.

**Postgres support alongside H2.** `docker-compose.yml` starts a Postgres 16 container with one command. `spring-dotenv` reads `.env` so credentials never live in source control.

**Live deployed demo at silicon-valley-trail.duckdns.org.** The cached distances on the server are warm so reviewers can get the full Medium and Impossible experience without waiting for OSRM.

---

## Final Notes

The main focus of this project is:
- clear game logic
- proper handling of external APIs
- clean separation of concerns
- zero setup friction (no API keys, no database to install)

Everything else is kept simple on purpose to stay within the timeline and avoid unnecessary complexity.
