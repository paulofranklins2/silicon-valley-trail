package com.pcunha.svt.application;

import com.pcunha.svt.domain.ActionOutcome;
import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.model.*;
import com.pcunha.svt.domain.port.WeatherPort;
import lombok.Getter;

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

    public TurnResult processTurn(GameState gameState, GameAction gameAction) {
        TurnResult turnResult = new TurnResult();
        turnResult.setGameAction(gameAction);

        int locationBefore = gameState.getJourneyState().getCurrentLocationIndex();

        ActionResult actionResult = actionHandler.handle(gameState, gameAction);
        turnResult.setActionOutcome(actionResult.outcome());
        turnResult.setChosenRoll(actionResult.chosenRoll());

        // if action was blocked (e.g. exhausted), skip the rest of the turn
        if (actionResult.outcome() == ActionOutcome.EXHAUSTED) {
            // still fetch weather for display
            loadWeather(gameState, turnResult);
            return turnResult;
        }

        // fetch weather
        WeatherSignal weatherSignal = loadWeather(gameState, turnResult);

        // weather effects only apply when traveling,
        // this prevents users to find a good weather and exploit it
        if (gameAction == GameAction.TRAVEL) {
            applyWeatherEffects(gameState, weatherSignal);
        }

        // Arriving at the final destination skips the event, victory is the story beat.
        int locationAfter = gameState.getJourneyState().getCurrentLocationIndex();
        boolean arrivedAtIntermediateCity = locationAfter > locationBefore
                && !gameState.getJourneyState().hasReachedDestination();
        if (arrivedAtIntermediateCity) {
            GameEvent event = eventProcessor.generateEvent(weatherSignal);
            turnResult.setGameEvent(event);

            // if event has choices, pause and wait for player decision
            if (event.getOutcomes() != null && !event.getOutcomes().isEmpty()) {
                gameState.getProgressState().setPendingEvent(event);
                return turnResult;
            }

            eventProcessor.applyEvent(gameState, event);
        }

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
