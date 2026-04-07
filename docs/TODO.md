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
- [x] Game mode selection on start page (Fast / Road / Walking)
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
- [x] Leaderboard page with separate Fast / Road / Walking rankings
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
- [x] Four game modes (Fast, Road, Walking, Walking+) with speed multiplier
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
- [ ] Quick start from a fresh machine
- [ ] API keys / mock instructions
- [ ] Architecture overview
- [ ] Dependency list
- [ ] How to run tests
- [ ] Example gameplay
- [ ] AI disclosure

### Documentation - REQUIRED
- [ ] Live URL or screen recording
- [x] Update DESIGN.md with final state
