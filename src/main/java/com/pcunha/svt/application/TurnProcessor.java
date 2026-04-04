package com.pcunha.svt.application;

import com.pcunha.svt.domain.ActionOutcome;
import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.model.GameEvent;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.WeatherSignal;
import com.pcunha.svt.domain.port.WeatherPort;
import lombok.Getter;

import java.util.Random;

public class TurnProcessor {
    private final ActionHandler actionHandler;
    private final ConditionEvaluator conditionEvaluator;
    @Getter
    private final EventProcessor eventProcessor;
    private final Random random;
    private final static double EVENT_CHANCE = 0.2;
    private final WeatherPort weatherPort;

    // const weather change
    private static final int RAINY_CHANGE_HEALTH = -5;
    private static final int RAINY_CHANGE_ENERGY = -2;
    private static final int STORMY_CHANGE_ENERGY = -10;
    private static final int STORMY_CHANGE_FOOD = -1;
    private static final int HEATWAVE_CHANGE_MORALE = -5;
    private static final int HEATWAVE_CHANGE_HEALTH = -3;
    private static final int CLEAR_CHANGE_HEALTH = 2;
    private static final int CLEAR_CHANGE_ENERGY = 5;

    public TurnProcessor(ActionHandler actionHandler, ConditionEvaluator conditionEvaluator, EventProcessor eventProcessor, Random random, WeatherPort weatherPort) {
        this.actionHandler = actionHandler;
        this.conditionEvaluator = conditionEvaluator;
        this.eventProcessor = eventProcessor;
        this.random = random;
        this.weatherPort = weatherPort;
    }

    public void processTurn(GameState gameState, GameAction gameAction) {
        gameState.setLastAction(gameAction);
        actionHandler.handle(gameState, gameAction);

        // if action was blocked (e.g. exhausted), skip the rest of the turn
        if (gameState.getLastActionResult() == ActionOutcome.EXHAUSTED) {
            // still fetch weather for display
            fetchWeather(gameState);
            gameState.setLastEvent(null);
            return;
        }

        // fetch weather
        WeatherSignal weatherSignal = fetchWeather(gameState);

        // weather effects only apply when traveling,
        // this prevents users to find a good weather and exploit it
        if (gameAction == GameAction.TRAVEL) {
            applyWeatherEffects(gameState, weatherSignal);
        }

        // random events
        if (random.nextDouble() < EVENT_CHANCE) {
            GameEvent event = eventProcessor.generateEvent(gameState, weatherSignal);
            gameState.setLastEvent(event);

            // if event has choices, pause and wait for player decision
            if (event.getOutcomes() != null && !event.getOutcomes().isEmpty()) {
                gameState.setWaitingEventChoice(true);
                return;
            }

            eventProcessor.applyEvent(gameState, event);
        } else {
            gameState.setLastEvent(null);
        }

        conditionEvaluator.evaluate(gameState);
        if (!gameState.isGameOver()) {
            gameState.nextTurn();
        }
    }

    private WeatherSignal fetchWeather(GameState gameState) {
        WeatherSignal weatherSignal = weatherPort.getWeather(gameState.getJourneyState().getCurrentLocation());
        gameState.setLastWeather(weatherSignal.getWeatherCategory());
        gameState.setLastWeatherTemp(weatherSignal.getTemperature());
        return weatherSignal;
    }

    public void resolveChoice(GameState gameState, int choiceIndex) {
        GameEvent event = gameState.getLastEvent();
        if (event == null || event.getOutcomes() == null) return;
        if (choiceIndex < 0 || choiceIndex >= event.getOutcomes().size()) return;

        eventProcessor.applyOutcome(gameState, event.getOutcomes().get(choiceIndex));
        gameState.setWaitingEventChoice(false);

        conditionEvaluator.evaluate(gameState);
        if (!gameState.isGameOver()) {
            gameState.nextTurn();
        }
    }

    private void applyWeatherEffects(GameState gameState, WeatherSignal weatherSignal) {
        if (weatherSignal == null) return;

        switch (weatherSignal.getWeatherCategory()) {
            case RAINY -> {
                gameState.getTeamState().changeHealth(RAINY_CHANGE_HEALTH);
                gameState.getTeamState().changeEnergy(RAINY_CHANGE_ENERGY);
            }
            case STORMY -> {
                gameState.getTeamState().changeEnergy(STORMY_CHANGE_ENERGY);
                gameState.getResourceState().changeFood(STORMY_CHANGE_FOOD);
            }
            case HEATWAVE -> {
                gameState.getTeamState().changeMorale(HEATWAVE_CHANGE_MORALE);
                gameState.getTeamState().changeHealth(HEATWAVE_CHANGE_HEALTH);
            }
            case CLEAR -> {
                gameState.getTeamState().changeHealth(CLEAR_CHANGE_HEALTH);
                gameState.getTeamState().changeEnergy(CLEAR_CHANGE_ENERGY);
            }
        }
    }
}
