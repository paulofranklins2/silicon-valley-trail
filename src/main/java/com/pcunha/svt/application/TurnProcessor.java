package com.pcunha.svt.application;

import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.GameState;

public class TurnProcessor {
    private final ActionHandler actionHandler;
    private final ConditionEvaluator conditionEvaluator;

    public TurnProcessor(ActionHandler actionHandler, ConditionEvaluator conditionEvaluator) {
        this.actionHandler = actionHandler;
        this.conditionEvaluator = conditionEvaluator;
    }

    public void processTurn(GameState gameState, GameAction gameAction) {
        actionHandler.handle(gameState, gameAction);
        conditionEvaluator.evaluate(gameState);
        if (!gameState.isGameOver()) {
            gameState.nextTurn();
        }
    }
}
