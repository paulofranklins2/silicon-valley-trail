# TODO - Silicon Valley Trail

> [Design Document](DESIGN.md)

## Goal
Build Silicon Valley Trail with clean backend logic, Spring Boot + Thymeleaf web UI, H2 persistence, and real API integration.

---

## Day 1 - Domain models + validation + tests

### Must have
- [x] Create `domain/model/TeamState` (health, energy, morale clamped 0-100)
- [x] Create `domain/model/ResourceState` (cash, food, computeCredits floor at 0)
- [x] Create `domain/model/JourneyState` (currentLocationIndex, distanceToNext, totalDistance)
- [x] Create `domain/model/Location` (name, lat, long)
- [x] Create `domain/model/GameState` (composes above + turn, gameOver, victory, market state, game mode)
- [x] Create `domain/model/GameEvent`, `EventOutcome`, `WeatherSignal`, `ActionInfo`
- [x] Create enums: `GameAction`, `EventCategory`, `ActionOutcome`, `WeatherCategory`, `LossReason`, `StatType`, `GameMode`
- [x] Create ports: `WeatherPort`, `DistancePort`, `LeaderboardPort`

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
- [x] Create `application/GameEngine` (game mode support, batch distance calculation)
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

### Distance API (Haversine + OSRM)
- [x] Create `HaversineDistanceAdapter` (straight-line, pure math)
- [x] Create `OsrmDistanceAdapter` (real driving distance, batch API call, Haversine fallback)
- [x] Create `MockDistanceAdapter`
- [x] `DistancePort.calculateLegDistances()` default method eliminates duplication
- [x] Game mode selection: Fast (Haversine) vs Road (OSRM)

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
- [x] GameController with AJAX endpoints (action, choice, market, leaderboard)
- [x] Game mode selection on start page (Fast / Road)
- [x] Action cards rendered from backend data (no hardcoded frontend values)
- [x] Energy costs read from `data-energy-cost` attribute
- [x] Grace period thresholds from `window.__gameConfig`
- [x] City market modal (voluntary, per-city persistence on GameState, sold-out tracking)
- [x] Choice modal with outcome tags and current stats
- [x] Weather display with initial weather on game start
- [x] Journey map with total distance and game mode label
- [x] Grace period warnings (pulsing badges: starving X/2, broke X/3)
- [x] Leaderboard page with separate Fast / Road rankings
- [x] End screen with score submission and loss reason
- [x] Maven Wrapper for zero-install builds

---

## Day 6 - Polish + documentation

### Gameplay
- [x] Energy gate: actions blocked when insufficient energy
- [x] Compute penalty: travel halved at 0 compute
- [x] Market: cash validation, purchase limits
- [x] Weather only on travel (prevents exploit)
- [x] Game balance: health/morale drain from actions and events
- [x] Leaderboard with weighted score (victory, turn efficiency, stats, resources)
- [x] Two game modes with separate leaderboards
- [ ] Playtest full game and tune balance
- [ ] Verify app runs from scratch with `./mvnw spring-boot:run`
- [ ] Delete `PersistencePort` (unused)

### README - REQUIRED
- [ ] Quick start from a fresh machine
- [ ] API keys / mock instructions
- [ ] Architecture overview
- [ ] Dependency list
- [ ] How to run tests
- [ ] Example gameplay
- [ ] AI disclosure

### Documentation - REQUIRED
- [ ] Live URL or screen recording
- [ ] Update DESIGN.md with final state
- [ ] Add `.env.example` file
