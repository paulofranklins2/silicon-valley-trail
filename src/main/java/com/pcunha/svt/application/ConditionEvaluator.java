package com.pcunha.svt.application;

import com.pcunha.svt.domain.LossReason;
import com.pcunha.svt.domain.model.*;

public class ConditionEvaluator {
    private final Tunables tunables;

    public ConditionEvaluator(Tunables tunables) {
        this.tunables = tunables;
    }

    public void evaluate(GameState gameState) {
        if (hasWon(gameState)) {
            gameState.getEndingState().markVictory();
            stampEndTime(gameState);
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

        updateResourceCounters(gameState);

        ResourceGraceState graceState = gameState.getGraceState();
        if (graceState.getTurnWithoutFood() > tunables.foodGraceTurns()) {
            lose(gameState, LossReason.STARVATION);
            return;
        }

        if (graceState.getTurnWithoutCash() > tunables.cashGraceTurns()) {
            lose(gameState, LossReason.NO_CASH);
        }
    }

    private void updateResourceCounters(GameState gameState) {
        ResourceState resourceState = gameState.getResourceState();
        ResourceGraceState graceState = gameState.getGraceState();

        if (resourceState.getFood() <= 0) {
            graceState.incrementTurnWithoutFood();
        } else {
            graceState.resetTurnWithoutFood();
        }

        if (resourceState.getCash() <= 0) {
            graceState.incrementTurnWithoutCash();
        } else {
            graceState.resetTurnWithoutCash();
        }
    }

    private boolean hasWon(GameState gameState) {
        JourneyState journeyState = gameState.getJourneyState();
        return journeyState.hasReachedDestination();
    }

    private void lose(GameState gameState, LossReason lossReason) {
        gameState.getEndingState().setLossReason(lossReason);
        gameState.getEndingState().setGameOver(true);
        stampEndTime(gameState);
    }

    private void stampEndTime(GameState gameState) {
        if (gameState.getProgressState().getEndTimeMs() == 0) {
            gameState.getProgressState().setEndTimeMs(System.currentTimeMillis());
        }
    }
}
