package com.pcunha.svt.application;

import com.pcunha.svt.domain.LossReason;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.JourneyState;
import com.pcunha.svt.domain.model.ResourceState;
import com.pcunha.svt.domain.model.TeamState;

public class ConditionEvaluator {
    private static final int FOOD_GRACE_TURNS = 2;
    private static final int CASH_GRACE_TURNS = 3;

    public void evaluate(GameState gameState) {
        if (hasWon(gameState)) {
            gameState.setVictory(true);
            return;
        }

        TeamState teamState = gameState.getTeamState();

        if (teamState.getHealth() <= 0) {
            lose(gameState, LossReason.POOR_HEALTH);
            return;
        }

        if (teamState.getMorale() <= 0) {
            lose(gameState, LossReason.POOR_MORALE);
            return;
        }

        if (gameState.getTurnWithoutFood() >= FOOD_GRACE_TURNS) {
            lose(gameState, LossReason.STARVATION);
            return;
        }

        if (gameState.getTurnWithoutCash() >= CASH_GRACE_TURNS) {
            lose(gameState, LossReason.NO_CASH);
            return;
        }

        updateResourceCounters(gameState);
    }

    private void updateResourceCounters(GameState gameState) {
        ResourceState resourceState = gameState.getResourceState();

        if (resourceState.getFood() <= 0) {
            gameState.incrementTurnWithoutFood();
        } else {
            gameState.resetTurnWithoutFood();
        }

        if (resourceState.getCash() <= 0) {
            gameState.incrementTurnWithoutCash();
        } else {
            gameState.resetTurnWithoutCash();
        }
    }

    private boolean hasWon(GameState gameState) {
        JourneyState journeyState = gameState.getJourneyState();
        return journeyState.hasReachedDestination();
    }

    private void lose(GameState gameState, LossReason lossReason) {
        gameState.setLossReason(lossReason);
        gameState.setGameOver(true);
    }
}
