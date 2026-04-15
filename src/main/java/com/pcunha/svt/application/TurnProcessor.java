package com.pcunha.svt.application;

import com.pcunha.svt.domain.ActionOutcome;
import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.model.*;
import com.pcunha.svt.domain.port.WeatherPort;
import lombok.Getter;

import java.util.Random;

public class TurnProcessor {
    private final ActionHandler actionHandler;
    private final ConditionEvaluator conditionEvaluator;
    @Getter
    private final EventProcessor eventProcessor;
    private final WeatherPort weatherPort;
    private final Tunables tunables;

    public TurnProcessor(ActionHandler actionHandler, ConditionEvaluator conditionEvaluator, EventProcessor eventProcessor, WeatherPort weatherPort, Tunables tunables) {
        this.actionHandler = actionHandler;
        this.conditionEvaluator = conditionEvaluator;
        this.eventProcessor = eventProcessor;
        this.weatherPort = weatherPort;
        this.tunables = tunables;
    }

    // Checks if the game uses a deterministic seed (Daily mode)
    private boolean isSeeded(GameState gameState) {
        return gameState.getSeed() != 0;
    }

    // Builds a seeded Random for a specific purpose so action rolls and events each get their own sequence
    // salt is a borrowed name from cryptography nothing else is just to make each action different values
    private Random seededRandom(long seed, int turn, int salt) {
        return new Random(seed * 31 + turn * 7 + salt);
    }

    /**
     * Processes a single game turn. Steps:
     * 1. Roll the action (travel distance, scavenge loot, etc.)
     * 2. Fetch weather for the current location
     * 3. Apply weather effects if traveling
     * 4. If the player arrived at an intermediate city, generate a random event
     * 5. Check end-game conditions (death, victory)
     * 6. Advance the turn counter
     *
     * In Daily mode each step that uses randomness gets its own seeded Random
     * (salt 1 for actions, salt 2 for events) so different player choices
     * can't shift the random sequence of another step.
     */
    public TurnResult processTurn(GameState gameState, GameAction gameAction) {
        TurnResult turnResult = new TurnResult();
        turnResult.setGameAction(gameAction);

        long seed = gameState.getSeed();
        int turn = gameState.getProgressState().getTurn();

        int locationBefore = gameState.getJourneyState().getCurrentLocationIndex();

        // Step 1: Roll action — seeded Random (salt 1) keeps action rolls isolated from events
        ActionResult actionResult;
        if (isSeeded(gameState)) {
            actionResult = actionHandler.handle(gameState, gameAction, seededRandom(seed, turn, 1));
        } else {
            actionResult = actionHandler.handle(gameState, gameAction);
        }
        turnResult.setActionOutcome(actionResult.outcome());
        turnResult.setChosenRoll(actionResult.chosenRoll());

        // Step 1b: If the player has no energy the action is blocked, skip the rest
        if (actionResult.outcome() == ActionOutcome.EXHAUSTED) {
            loadWeather(gameState, turnResult);
            return turnResult;
        }

        // Step 2: Fetch weather for the player's current location
        WeatherSignal weatherSignal = loadWeather(gameState, turnResult);

        // Step 3: Apply weather stat changes only when traveling (prevents exploit of resting on good weather)
        if (gameAction == GameAction.TRAVEL) {
            applyWeatherEffects(gameState, weatherSignal);
        }

        // Step 4: Generate a random event if the player arrived at an intermediate city
        int locationAfter = gameState.getJourneyState().getCurrentLocationIndex();
        boolean arrivedAtIntermediateCity = locationAfter > locationBefore
                && !gameState.getJourneyState().hasReachedDestination();
        if (arrivedAtIntermediateCity) {
            int cityIndex = gameState.getJourneyState().getCurrentLocationIndex();
            // Seeded Random (salt 2) so events are independent of which action was taken
            GameEvent event;
            if (isSeeded(gameState)) {
                event = eventProcessor.generateEvent(weatherSignal, cityIndex, seededRandom(seed, turn, 2));
            } else {
                event = eventProcessor.generateEvent(weatherSignal, cityIndex);
            }
            turnResult.setGameEvent(event);

            // If event has choices, pause and wait for player decision
            if (event.getOutcomes() != null && !event.getOutcomes().isEmpty()) {
                gameState.getProgressState().setPendingEvent(event);
                return turnResult;
            }

            eventProcessor.applyEvent(gameState, event);
        }

        // Step 5 & 6: Check end-game conditions, then advance the turn counter
        conditionEvaluator.evaluate(gameState);
        if (!gameState.getEndingState().isGameOver()) {
            gameState.getProgressState().nextTurn();
        }
        return turnResult;
    }

    public void loadInitialWeather(GameState gameState) {
        WeatherSignal signal = weatherPort.getWeather(gameState.getJourneyState().getCurrentLocation());
        ProgressState progress = gameState.getProgressState();
        progress.setCurrentWeather(signal.weatherCategory());
        progress.setCurrentWeatherTemperature(signal.temperature());
    }

    private WeatherSignal loadWeather(GameState gameState, TurnResult turnResult) {
        WeatherSignal weatherSignal = weatherPort.getWeather(gameState.getJourneyState().getCurrentLocation());
        turnResult.setWeatherCategory(weatherSignal.weatherCategory());
        turnResult.setWeatherTemperature(weatherSignal.temperature());
        // cache on state so the UI can render current weather on non-turn page loads
        ProgressState progress = gameState.getProgressState();
        progress.setCurrentWeather(weatherSignal.weatherCategory());
        progress.setCurrentWeatherTemperature(weatherSignal.temperature());
        return weatherSignal;
    }

    public TurnResult resolveChoice(GameState gameState, int choiceIndex) {
        ProgressState progress = gameState.getProgressState();
        GameEvent event = progress.getPendingEvent();
        if (event == null || event.getOutcomes() == null) return new TurnResult();
        if (choiceIndex < 0 || choiceIndex >= event.getOutcomes().size()) return new TurnResult();

        eventProcessor.applyOutcome(gameState, event.getOutcomes().get(choiceIndex));
        progress.clearPendingEvent();

        TurnResult result = new TurnResult();
        result.setGameEvent(event);

        conditionEvaluator.evaluate(gameState);
        if (!gameState.getEndingState().isGameOver()) {
            progress.nextTurn();
        }
        return result;
    }

    private void applyWeatherEffects(GameState gameState, WeatherSignal weatherSignal) {
        if (weatherSignal == null) return;

        StatDelta delta = switch (weatherSignal.weatherCategory()) {
            case RAINY -> tunables.weather().rainy();
            case STORMY -> tunables.weather().stormy();
            case HEATWAVE -> tunables.weather().heatwave();
            case CLEAR -> tunables.weather().clear();
        };

        gameState.getTeamState().changeHealth(delta.health());
        gameState.getTeamState().changeEnergy(delta.energy());
        gameState.getTeamState().changeMorale(delta.morale());
        gameState.getResourceState().changeFood(delta.food());
    }
}
