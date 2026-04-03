package com.pcunha.svt.application;

import com.pcunha.svt.domain.model.GameState;

public class ConditionEvaluator {

    public void evaluate(GameState gameState) {
        if (gameState.getJourneyState().hasReachedDestination()) {
            gameState.setVictory(true);
            return;
        }
        if (gameState.getTeamState().getHealth() <= 0) {
            gameState.setGameOver(true);
        }
        if (gameState.getTeamState().getMorale() <= 0) {
            gameState.setGameOver(true);
        }
    }
}
