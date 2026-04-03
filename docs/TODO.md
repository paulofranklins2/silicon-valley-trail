# TODO - Silicon Valley Trail

> [Design Document](DESIGN.md)

## Goal
Build Silicon Valley Trail with clean backend logic, Spring Boot + Thymeleaf web UI, H2 persistence, and real API integration.

---

## Day 1 - Domain models + validation + tests

### Must have
- [x] Create `domain/model/TeamState` (health, energy, morale clamped 0-100)
- [x] Create `domain/model/ResourceState` (cash, food, computeCredits floor at 0)
- [x] Create `domain/model/JourneyState` (currentLocationIndex, distanceToNext)
- [x] Create `domain/model/Location` (name, lat, long)
- [x] Create `domain/model/GameState` (composes TeamState, ResourceState, JourneyState + turn, gameOver, victory)
- [x] Create `domain/model/GameEvent`
- [x] Create `domain/model/EventOutcome`
- [x] Create `domain/model/WeatherSignal`
- [x] Create `domain/GameAction` enum
- [x] Create `domain/EventCategory` enum
- [x] Create `domain/ActionOutcome` enum
- [x] Create `domain/WeatherCategory` enum
- [x] Create `domain/port/WeatherPort`
- [x] Create `domain/port/DistancePort`
- [x] Create `domain/port/PersistencePort`

### Tests
- [x] Test TeamState clamping
- [x] Test ResourceState floor
- [x] Test JourneyState progress logic
- [x] Test multi-location travel (while loop fix)

---

## Day 2 - Core game loop

### Must have
- [x] Create `application/ActionHandler`
- [x] Create `application/ConditionEvaluator`
- [x] Create `application/TurnProcessor`
- [x] Create `application/GameEngine`

### Tests
- [x] Test each action produces correct resource changes
- [x] Test win condition
- [x] Test loss conditions (health, morale)
- [x] Test turn flow with known state

---

## Day 3 - Event system

### Must have
- [x] Create `application/EventProcessor`
- [x] Add initial event pool across all 5 categories (15 events)
- [x] Add random event chance (20% per turn, not every turn)
- [x] Add events with player choices (5 choice events across all categories)

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

### Weather API
- [x] Create `infrastructure/api/OpenMeteoAdapter`
- [x] Parse weather response into `WeatherSignal`
- [x] Add 3-second timeout
- [x] Create `infrastructure/api/MockWeatherAdapter`
- [x] Create `infrastructure/api/DemoWeatherAdapter`
- [x] Fallback to mock on failure
- [x] Create `domain/WeatherCategory` enum
- [x] Wire weather into gameplay (TurnProcessor applies stat changes per weather type)

### Distance
- [x] Create `infrastructure/api/HaversineDistanceAdapter`
- [x] Wire into GameEngine for real distances at startup
- [x] Create `infrastructure/api/MockDistanceAdapter`

### Tests
- [x] Test Haversine returns valid distance
- [ ] Test fallback triggers on API failure
- [ ] Test mock returns valid data

---

## Day 5 - Web UI

### Must have
- [x] Create `infrastructure/web/GameController`
- [x] Create `infrastructure/web/GameConfig` (Spring bean wiring)
- [x] GET `/` to show start page
- [x] POST `/start` to create game in session
- [x] POST `/action` to process turn
- [x] POST `/api/action` to process turn via AJAX (returns JSON)
- [x] POST `/api/choice` to resolve event choices via AJAX
- [x] GET `/game` to show current state
- [x] GET `/end` to show victory/defeat
- [x] Null session guards on all routes (redirect to / if no game)
- [x] Create templates (start, game, end) with Thymeleaf fragments
- [x] Split CSS into modular files (13 files)
- [x] Split JS into modules (toast, stats, actions, journey, animations, weather)
- [x] AJAX action processing without page refresh
- [x] Toast notifications for action feedback
- [x] Journey map with proportional city dots and animated train
- [x] Action scenes with character sprite animations
- [x] Team display with status effects based on stats
- [x] Choice modal with outcome tags and current stats display
- [x] Weather effects UI (rain, storm, heatwave animations + temperature)
- [x] Kenney game assets (characters, food icons, train)

---

## Day 6 - Polish + documentation

### Gameplay - REQUIRED
- [x] Wire weather API into actual gameplay (TurnProcessor applies stat effects per weather type)
- [ ] Add cash/food loss conditions to ConditionEvaluator (requirement: "cash hits zero", resource depletion)
- [ ] Playtest full game and tune balance (food drain, energy costs, event chance, travel distances)
- [ ] Verify app runs from scratch with `mvn spring-boot:run`
- [ ] Final cleanup on naming/comments/dead code

### README - REQUIRED
- [ ] Quick start from a fresh machine (Java 21 + Maven prerequisites, clone, run)
- [ ] How to set API keys / how to run with mocks (Open-Meteo needs no key, `game.weather.mode` config)
- [ ] Brief architecture overview (hexagonal: domain/application/infrastructure)
- [ ] Dependency list with versions
- [ ] How to run tests (`mvn test`)
- [ ] Example gameplay flow (sample turn sequence)
- [ ] AI disclosure (how AI was used in development)

### Documentation - REQUIRED
- [ ] Record screen capture or provide URL to working app
- [ ] Finalize DESIGN.md with final state (choices implemented, weather wired in, updated tradeoffs)
- [ ] Add `.env.example` file (even though Open-Meteo needs no key)

### Nice to have
- [ ] Weather-conditional events
- [ ] Grace period loss conditions (food=0 for 2 consecutive turns... game over)
- [ ] More events to reduce repetition across 10 locations
- [ ] Weighted event selection (low morale... more team events, low cash... more market events)
- [ ] H2 persistence (I could maybe implement a ranking system where user can enter their name or just save teamname)
- [ ] Second API integration (maybe osrm for longer events instead of using Haversine, create a OpenSource Route Machine/Nominatim mode)