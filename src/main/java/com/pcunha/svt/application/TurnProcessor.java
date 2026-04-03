package com.pcunha.svt.application;

import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.GameEvent;
import com.pcunha.svt.domain.GameState;

import java.util.Random;

public class TurnProcessor {
    private final ActionHandler actionHandler;
    private final ConditionEvaluator conditionEvaluator;
    private final EventProcessor eventProcessor;
    private final Random random;
    private final static double EVENT_CHANCE = 0.2;

    public TurnProcessor(ActionHandler actionHandler, ConditionEvaluator conditionEvaluator, EventProcessor eventProcessor, Random random) {
        this.actionHandler = actionHandler;
        this.conditionEvaluator = conditionEvaluator;
        this.eventProcessor = eventProcessor;
        this.random = random;
    }

    public void processTurn(GameState gameState, GameAction gameAction) {
        gameState.setLastAction(gameAction);
        actionHandler.handle(gameState, gameAction);

        if (random.nextDouble() < EVENT_CHANCE) {
            GameEvent event = eventProcessor.generateEvent(gameState, null);
            gameState.setLastEvent(event);
            eventProcessor.applyEvent(gameState, event);
        } else {
            gameState.setLastEvent(null);
        }

        conditionEvaluator.evaluate(gameState);
        if (!gameState.isGameOver()) {
            gameState.nextTurn();
        }
    }
}
