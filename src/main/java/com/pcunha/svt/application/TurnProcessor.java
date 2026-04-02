package com.pcunha.svt.application;

import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.GameEvent;
import com.pcunha.svt.domain.GameState;

public class TurnProcessor {
    private final ActionHandler actionHandler;
    private final ConditionEvaluator conditionEvaluator;
    private final EventProcessor eventProcessor;

    public TurnProcessor(ActionHandler actionHandler, ConditionEvaluator conditionEvaluator, EventProcessor eventProcessor) {
        this.actionHandler = actionHandler;
        this.conditionEvaluator = conditionEvaluator;
        this.eventProcessor = eventProcessor;
    }

    public void processTurn(GameState gameState, GameAction gameAction) {
        actionHandler.handle(gameState, gameAction);
        GameEvent event = eventProcessor.generateEvent(gameState, null);
        gameState.setLastAction(gameAction);
        gameState.setLastEvent(event);
        eventProcessor.applyEvent(gameState, event);
        conditionEvaluator.evaluate(gameState);
        if (!gameState.isGameOver()) {
            gameState.nextTurn();
        }
    }
}
