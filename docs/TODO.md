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
- [x] Create `domain/action/GameAction` enum
- [x] Create `domain/event/GameEvent`
- [x] Create `domain/event/EventCategory` enum
- [x] Create `domain/event/EventOutcome`
- [x] Create `domain/port/WeatherPort`
- [x] Create `domain/port/DistancePort`
- [x] Create `domain/port/PersistencePort`

### Tests
- [x] Test TeamState clamping
- [x] Test ResourceState floor
- [x] Test JourneyState progress logic

---

## Day 2 - Core game loop

### Must have
- [x] Create `application/ActionHandler`
- [X] Create `application/ConditionEvaluator`
- [x] Create `application/TurnProcessor`
- [x] Create `application/GameEngine`

### Tests
- [x] Test each action produces correct resource changes
- [x] Test win condition
- [x] Test loss conditions
- [x] Test turn flow with known state

---

## Day 3 - Event system

### Must have
- [ ] Create `application/EventProcessor`
- [ ] Add initial event pool across all 5 categories
- [ ] Add at least 2-3 events with player choices
- [ ] Wire weather signal into category weights
- [ ] Wire game state into category weights

### Tests
- [ ] Test event deltas apply correctly
- [ ] Test weighted selection with seeded random
- [ ] Test weather signal changes event weighting

### Nice to have
- [ ] Expand event pool if the game feels repetitive

---

## Day 4 - API integration

### Weather API
- [ ] Create `infrastructure/api/OpenMeteoAdapter`
- [ ] Parse weather response into `WeatherSignal`
- [ ] Add 3-second timeout
- [ ] Create `infrastructure/api/MockWeatherAdapter`
- [ ] Fallback to mock on failure

### Distance API
- [ ] Create `infrastructure/api/NominatimAdapter`
- [ ] Fetch real distances between locations at startup
- [ ] Create `infrastructure/api/MockDistanceAdapter`

### Tests
- [ ] Test fallback triggers on API failure
- [ ] Test timeout is respected
- [ ] Test mock returns valid data

---

## Day 5 - Web UI

### Must have
- [ ] Create `infrastructure/web/GameController`
- [ ] GET `/` to show start page
- [ ] POST `/start` to create game in session
- [ ] POST `/action` to process turn
- [ ] GET `/game` to show current state
- [ ] Create `templates/start.html`
- [ ] Create `templates/game.html`
- [ ] Create `templates/end.html`
- [ ] Create `static/css/game.css`

### Nice to have
- [ ] Create `templates/event.html`

---

## Day 6 - Persistence + polish

### Persistence
- [ ] Create `SavedGame`, `TeamSnapshot`, `ResourceSnapshot`, `JourneySnapshot`
- [ ] Create `SavedGameRepository`
- [ ] Create `JpaPersistenceAdapter`
- [ ] Map domain state to entities and back

### Polish
- [ ] Play through the full game a few times and tune balance
- [ ] Add more events if needed
- [ ] Add edge case tests
- [ ] Final cleanup on naming/comments/dead code

---

## Day 7 - Documentation

- [ ] Write README.md (quick start, how to run, architecture, tests, example gameplay flow)
- [ ] Finalize DESIGN.md
- [ ] Record a short playthrough
- [ ] Verify app runs from scratch with `mvn spring-boot:run`

### If time left
- [ ] Deploy app (Cloudflare Pages or tunnel)
- [ ] Expose app externally via VPN/tunnel