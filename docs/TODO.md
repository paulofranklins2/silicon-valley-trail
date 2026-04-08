# TODO - Silicon Valley Trail

> [Design Document](DESIGN.md)

## Goal
Build Silicon Valley Trail with clean backend logic, Spring Boot + Thymeleaf web UI, H2/Postgres persistence, and real API integration.

---

## Day 1 - Domain models + validation + tests

### Must have
- [x] Create `domain/model/TeamState` (health, energy, morale clamped 0-100)
- [x] Create `domain/model/ResourceState` (cash, food, computeCredits floor at 0)
- [x] Create `domain/model/JourneyState` (currentLocationIndex, distanceToNext, totalDistance)
- [x] Create `domain/model/Location` (name, lat, long)
- [x] Create `domain/model/GameState` (composes above + turn, gameOver, victory, market state, game mode)
- [x] Create `domain/model/GameEvent`, `EventOutcome`, `WeatherSignal`, `ActionInfo`, `MarketResult`, `DistanceResult`
- [x] Create enums: `GameAction`, `EventCategory`, `ActionOutcome`, `WeatherCategory`, `LossReason`, `StatType`, `GameMode`
- [x] Create ports: `WeatherPort`, `DistancePort` (with default `calculateLegDistances`), `LeaderboardPort`

### Tests
- [x] Test TeamState clamping
- [x] Test ResourceState floor
- [x] Test JourneyState progress logic
- [x] Test multi-location travel (while loop fix)

---

## Day 2 - Core game loop

### Must have
- [x] Create `application/ActionHandler` (energy gate, compute travel penalty, data-driven from YAML)
- [x] Create `application/ConditionEvaluator` (grace period loss for food/cash, counter updates after checks)
- [x] Create `application/TurnProcessor` (weather only on travel, fetchWeather helper)
- [x] Create `application/GameEngine` (pre-computed distances, market logic, retry support)
- [x] Create `application/ScoreCalculator` (weighted scoring for leaderboard)

### Tests
- [x] Test each action produces correct resource changes
- [x] Test win condition
- [x] Test loss conditions (health, morale)
- [x] Test loss conditions (starvation grace period, no cash grace period)
- [x] Test grace period counter reset when resource recovered
- [x] Test turn flow with known state
- [x] Test score calculation (victory high, defeat medium, early death low, faster > slower)

---

## Day 3 - Event system

### Must have
- [x] Create `application/EventProcessor` (loads from YAML)
- [x] Add event pool across 5 categories (20+ events)
- [x] Add random event chance (20% per turn)
- [x] Add events with player choices (5 choice events)
- [x] Add 5 city market variants (voluntary, per-city, purchase limits)
- [x] Market logic in GameEngine with MarketResult pattern

### Tests
- [x] Test event deltas apply correctly
- [x] Test event fires when random is below threshold
- [x] Test no event when random is above threshold
- [x] Test action still applies when no event fires

### Not implemented (would add with more time)
- [ ] Wire weather signal into category weights
- [ ] Wire game state into category weights

---

## Day 4 - API integration

### Weather API (Open-Meteo)
- [x] Create `OpenMeteoAdapter` with 3s timeout, Locale.US URL formatting
- [x] Create `MockWeatherAdapter`, `DemoWeatherAdapter`
- [x] Fallback to mock on failure
- [x] Wire weather into gameplay (travel only, prevents resting exploit)
- [x] Weather mode set to `api`
- [x] Weather pre-loaded at startup and cached for 1 hour per city

### Distance APIs (3 modes)
- [x] Create `HaversineDistanceAdapter` (Fast mode, pure math, always works)
- [x] Create `OsrmDistanceAdapter` (Road mode, driving distance, falls back to ORS car then Haversine)
- [x] Create `OpenRouteServiceAdapter` (Walking mode + Road fallback, supports car and foot profiles)
- [x] Create `MockDistanceAdapter`
- [x] `DistancePort.calculateLegDistances()` default method eliminates duplication
- [x] Pre-compute all distances at startup, cache per game mode
- [x] `DistanceResult` record signals whether fallback was used
- [x] Fallback chain: OSRM to ORS car to Haversine (passes through usedFallback flag)
- [x] Walking mode disabled when no ORS_API_KEY
- [x] Retry endpoint for failed modes (`POST /api/retry-distances`)

### Data-driven content
- [x] Create `GameDataLoader` (YAML deserialization)
- [x] Move events, markets, locations, actions to YAML files
- [x] `StatType` enum for type-safe stat references

### Tests
- [x] Test Haversine returns valid distance
- [x] Test OSRM returns valid distance for real locations
- [x] Test OSRM fallback on invalid coordinates
- [x] Test OSRM road distance >= straight-line distance
- [x] Test OpenMeteo fallback on invalid location
- [x] Test MockWeatherAdapter (valid range, deterministic, varied)
- [x] Test DemoWeatherAdapter (cycles, wraps, correct temps)
- [x] Test MockDistanceAdapter (fixed fallback)
- [x] Test GameDataLoader (events, locations, markets)

---

## Day 5 - Web UI

### Must have
- [x] GameController (thin routing, delegates to GameEngine)
- [x] Game mode selection on start page (Easy / Medium / Hard / Impossible / Daily)
- [x] Walking mode disabled on UI when no API key
- [x] Action cards rendered from backend data (no hardcoded frontend values)
- [x] Energy costs from `data-energy-cost` attribute
- [x] Grace period thresholds from `window.__gameConfig`
- [x] City market modal (voluntary, per-city persistence on GameState, sold-out tracking)
- [x] Choice modal with outcome tags and current stats
- [x] Fallback retry modal (retry API or accept Fast mode)
- [x] Weather display with initial weather on game start
- [x] Journey map with total distance and game mode label
- [x] Grace period warnings (pulsing badges: starving X/2, broke X/3)
- [x] Leaderboard page (originally per-mode top 5, now unified top 10 weighted, plus a daily view)
- [x] Leaderboard rows show outcome (Won / loss reason), location, turns, score
- [x] Filler rows pad each mode table to 5 slots (no layout shift on first submission)
- [x] Player name length capped at 10 chars (HTML maxlength + server-side validation)
- [x] Mobile leaderboard layout fix (`width: 100%` on table-wrap stops overflow)
- [x] 2x2 mode grid on desktop, single column under 768px
- [x] End screen with score submission and loss reason
- [x] Maven Wrapper for zero-install builds

---

## Day 6 - Polish + documentation

### Gameplay
- [x] Energy gate: actions blocked when insufficient energy
- [x] Compute penalty: travel halved at 0 compute
- [x] Market logic in GameEngine (not controller)
- [x] Weather only on travel (prevents exploit)
- [x] Game balance: health/morale drain from actions and events
- [x] Leaderboard with weighted score per game mode
- [x] Scoring parameters extracted to `scoring.yaml` (rebalance without recompile)
- [x] `ScoreCalculator` converted to `@Component` taking `Scoring` via DI
- [x] `ScoreInputs` record (per-run data) split from `Scoring` record (formula params)
- [x] `LeaderboardEntry` stores `lossReason`, `locationIndex`, `totalLocations` for recompute
- [x] `LeaderboardEntry.fromGameState` removed; build logic moved to `LeaderboardService.buildEntry`
- [x] Four game modes (Easy, Medium, Hard, Impossible) with speed multiplier
- [x] OSRM fallback to ORS car with fair ranking detection
- [x] Pre-computed distances at startup with shared port caching
- [x] Input validation: empty names, invalid actions, duplicate submissions
- [x] Deleted PersistencePort (unused)
- [x] Playtest full game and tune balance
- [x] Verify app runs from scratch with `./mvnw spring-boot:run`

### Infrastructure
- [x] Postgres support via env var (`DB_URL`)
- [x] spring-dotenv for `.env` file support
- [x] docker-compose.yml for local Postgres
- [x] `.env.example` with config documentation
- [x] Startup banner shows database type and mode status
- [x] Start page and leaderboard render modes dynamically from GameMode enum

### README - REQUIRED
- [x] Quick start from a fresh machine
- [x] API keys / mock instructions
- [x] Architecture overview
- [x] Dependency list
- [x] How to run tests
- [x] Example gameplay
- [x] AI disclosure

### Documentation - REQUIRED
- [x] Live URL or screen recording
- [x] Update DESIGN.md with final state

---

## Day 7 - Cross-device resume + daily mode + naming polish

### Mode rename + scoring
- [x] Rename game modes: FAST/ROAD/WALKING_FAST/WALKING_ROAD to EASY/MEDIUM/HARD/IMPOSSIBLE
- [x] Drop `description` field from `GameMode` (label is enough)
- [x] Add `scoreMultiplier` to `GameMode` (1.0 / 1.3 / 1.7 / 2.2 super-linear curve)
- [x] Consolidate leaderboard to single weighted top-10 (was per-mode top-5)
- [x] Store both `score` (raw) and `weightedScore` on `LeaderboardEntry`
- [x] Drop `getTopScores(GameMode)` from `LeaderboardPort`
- [x] Three-column score display in leaderboard table (Raw / Mult / Score)
- [x] Mode badge column on leaderboard rows

### Room + GameSession persistence layer
- [x] `Room` JPA entity with type/mode/seed/status/dateBucket
- [x] `GameSession` JPA entity with serialized GameState, playerToken, @Version
- [x] `RoomPort` + `GameSessionPort` in domain
- [x] `RoomAdapter` + `GameSessionAdapter` JPA implementations
- [x] `RoomService` lifecycle owner (createSoloGame, createOrJoinDailyGame, loadActiveSession, persist, markCompleted)
- [x] Java native serialization of GameState into a TEXT column via Base64 (12 domain classes implement Serializable)
- [x] No `@Lob`, no length-bound magic numbers, just `@Column(columnDefinition = "TEXT")`

### Cookie-based identity + cross-device resume
- [x] `PlayerCookies` utility, `svt_player` cookie, 30-day max-age, HttpOnly
- [x] Migrate `GameMvcController` from HttpSession to cookie + RoomService
- [x] Migrate `GameRestController` from HttpSession to cookie + RoomService
- [x] `/resume/{token}` endpoint with UUID validation
- [x] Continue button on home page when active session exists
- [x] Collapsible "Resume on another device" URL on home page

### Daily mode + daily leaderboard
- [x] `RoomType.DAILY` + `dateBucket` field on `Room`
- [x] `findOrCreateDailyRoom(mode, today)` with deterministic seed per (mode, date)
- [x] Daily mode is a 5th button in the mode selector (gameMode=DAILY magic value)
- [x] `dailyRun` boolean on `LeaderboardEntry`
- [x] `/leaderboard/daily` endpoint with today-only filter
- [x] All-time leaderboard explicitly filters out daily runs (`dailyRun=false`)
- [x] Tab toggle in leaderboard template between All-Time and Today's Daily

### Action outcome bundles (generic gambling layer)
- [x] Add `ActionEffect` top-level class (was nested as `ActionInfo.Effect`)
- [x] Add `ActionRoll` top-level class (wraps `List<ActionEffect>`, one bundle = one mutually exclusive outcome)
- [x] Add `ActionResult` record (returned by `ActionHandler.handle`, carries outcome enum + chosen roll)
- [x] `ActionInfo.outcomes` is `List<ActionRoll>` loaded from YAML alongside `effects`
- [x] `ActionHandler` generic flow: apply guaranteed effects, pick one outcome roll, apply its bundle
- [x] Drop the hardcoded per-action switch (was scavenge/pitchVcs custom logic)
- [x] `ActionHandler.classifyOutcome` is generic, full if statements, no ternaries
- [x] `ActionOutcome` enum slimmed to generic `SUCCESS`, `EXHAUSTED`, `GAIN`, `LOSS`
- [x] `TurnResult` gains `chosenRoll` field so the UI can introspect what fired

### YAML restructure for the bundle model
- [x] Travel: gambling action with morale -5 OR health -5 outcome rolls
- [x] Hackathon: jackpot bundle (+cash, +food) OR wreck bundle (-morale, -health)
- [x] Scavenge: food bundle OR (cash + morale) bundle
- [x] Pitch VCs: cash bundle OR (morale + health) bundle
- [x] Two new choice events in events.yml: Critical Production Bug, Conference Talk Slot

### Balance pass for fun-not-punishing
- [x] Travel: energy -15 to -10, compute -5 to -2, random morale/health -10 to -5
- [x] Rest: health +5 to +8, energy +10 to +15, morale +5 to +8
- [x] Scavenge: health -5 to -3, food +2 to +3, cash +10 to +15, morale -5 to -3
- [x] Hackathon: compute +12 to +15, energy -15 to -12, wreck stats -10 to -8
- [x] Pitch VCs: compute -5 to -3, guaranteed morale -3 to -2, failure morale -10 to -8, failure health -5 to -3
- [x] Starting resources in `GameEngine.createNewGame`: food 5 to 8, compute 5 to 8
- [x] Result: 2 travels + 1 rest is the sustainable cycle, 6 km of progress every 3 turns

### Action card UI for outcome rolls
- [x] Rewrite action card outcomes section to render `info.outcomes` as `action-card__roll` groups
- [x] CSS rule: "or" appears only between adjacent roll groups, not between every effect span
- [x] `animations.js` scavenge sprite picker reads `turnResult.chosenRoll.effects[0].stat` instead of per-action enum value
- [x] `animations.js` pitch celebration checks `actionOutcome === 'GAIN'` (was per-action `PITCH_SUCCESS`)
- [x] Drop dead `.action-card__fx .or-text` CSS rule

### Home page state fix
- [x] Split `hasActiveGame` into `canResume` (active and not over) and `hasUnfinishedEnd` (active and over but not submitted)
- [x] Show "Continue run" button only when game is not over
- [x] Show "View result" button when game is over and not yet submitted
- [x] Resume URL block stays visible whenever there is any active session

### Cleanups
- [x] Catch `OptimisticLockingFailureException` in action handler (graceful redirect on two-tab race)
- [x] `/resume/{token}` validates the token is a UUID before issuing the cookie
