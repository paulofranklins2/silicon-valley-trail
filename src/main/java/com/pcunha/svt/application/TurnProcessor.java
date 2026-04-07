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
    private final Random random;
    private final WeatherPort weatherPort;
    private final GameTunables tunables;

    public TurnProcessor(ActionHandler actionHandler, ConditionEvaluator conditionEvaluator, EventProcessor eventProcessor, Random random, WeatherPort weatherPort, GameTunables tunables) {
        this.actionHandler = actionHandler;
        this.conditionEvaluator = conditionEvaluator;
        this.eventProcessor = eventProcessor;
        this.random = random;
        this.weatherPort = weatherPort;
        this.tunables = tunables;
    }

    public TurnResult processTurn(GameState gameState, GameAction gameAction) {
        TurnResult turnResult = new TurnResult();
        turnResult.setGameAction(gameAction);

        ActionOutcome actionOutcome = actionHandler.handle(gameState, gameAction);
        turnResult.setActionOutcome(actionOutcome);

        // if action was blocked (e.g. exhausted), skip the rest of the turn
        if (turnResult.getActionOutcome() == ActionOutcome.EXHAUSTED) {
            // still fetch weather for display
            loadWeather(gameState, turnResult);
            gameState.getProgressState().setLastTurnResult(turnResult);
            return turnResult;
        }

        // fetch weather
        WeatherSignal weatherSignal = loadWeather(gameState, turnResult);

        // weather effects only apply when traveling,
        // this prevents users to find a good weather and exploit it
        if (gameAction == GameAction.TRAVEL) {
            applyWeatherEffects(gameState, weatherSignal);
        }

        // random events
        if (random.nextDouble() < tunables.eventChance()) {
            GameEvent event = eventProcessor.generateEvent(gameState, weatherSignal);
            turnResult.setGameEvent(event);

            // if event has choices, pause and wait for player decision
            if (event.getOutcomes() != null && !event.getOutcomes().isEmpty()) {
                turnResult.setWaitingEventChoice(true);
                gameState.getProgressState().setLastTurnResult(turnResult);
                return turnResult;
            }

            eventProcessor.applyEvent(gameState, event);
        }

        conditionEvaluator.evaluate(gameState);
        if (!gameState.getEndingState().isGameOver()) {
            gameState.getProgressState().nextTurn();
        }
        gameState.getProgressState().setLastTurnResult(turnResult);
        return turnResult;
    }

    public void loadInitialWeather(GameState gameState) {
        loadWeather(gameState, gameState.getProgressState().getLastTurnResult());
    }

    private WeatherSignal loadWeather(GameState gameState, TurnResult turnResult) {
        WeatherSignal weatherSignal = weatherPort.getWeather(gameState.getJourneyState().getCurrentLocation());
        turnResult.setWeatherCategory(weatherSignal.weatherCategory());
        turnResult.setWeatherTemperature(weatherSignal.temperature());
        return weatherSignal;
    }

    public void resolveChoice(GameState gameState, int choiceIndex) {
        GameEvent event = gameState.getProgressState().getLastTurnResult().getGameEvent();
        if (event == null || event.getOutcomes() == null) return;
        if (choiceIndex < 0 || choiceIndex >= event.getOutcomes().size()) return;

        eventProcessor.applyOutcome(gameState, event.getOutcomes().get(choiceIndex));
        gameState.getProgressState().getLastTurnResult().setWaitingEventChoice(false);

        conditionEvaluator.evaluate(gameState);
        if (!gameState.getEndingState().isGameOver()) {
            gameState.getProgressState().nextTurn();
        }
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
