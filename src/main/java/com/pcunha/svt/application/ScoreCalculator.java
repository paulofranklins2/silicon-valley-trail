package com.pcunha.svt.application;

import com.pcunha.svt.domain.model.GameState;

public class ScoreCalculator {
    private static final int VICTORY_BONUS = 1000;
    private static final int MAX_TURN_BONUS = 500;
    private static final int MAX_JOURNEY_BONUS = 300;
    private static final int TURN_BONUS_BASELINE = 30;

    // stat weights: harder to maintain = higher weight
    private static final double HEALTH_WEIGHT = 2.0;
    private static final double ENERGY_WEIGHT = 1.0;
    private static final double MORALE_WEIGHT = 3.0;

    // resource weights: scarcer = higher weight
    private static final double CASH_WEIGHT = 2.0;
    private static final double FOOD_WEIGHT = 3.0;
    private static final double COMPUTE_WEIGHT = 1.0;

    // resource normalization caps (beyond this, no extra score)
    private static final double CASH_CAP = 200.0;
    private static final double FOOD_CAP = 15.0;
    private static final double COMPUTE_CAP = 15.0;

    public static int calculate(GameState gameState) {
        int score = 0;

        if (gameState.getEndingState().isVictory()) {
            score += VICTORY_BONUS;
            score += turnEfficiencyBonus(gameState.getProgressState().getTurn());
        } else {
            score += journeyProgressBonus(gameState);
        }

        score += statScore(gameState);
        score += resourceScore(gameState);

        return Math.max(0, score);
    }

    private static int turnEfficiencyBonus(int turns) {
        // fewer turns = higher bonus, baseline at 30 turns (0 bonus)
        if (turns >= TURN_BONUS_BASELINE) return 0;
        double ratio = 1.0 - ((double) turns / TURN_BONUS_BASELINE);
        return (int) (ratio * MAX_TURN_BONUS);
    }

    private static int journeyProgressBonus(GameState gameState) {
        int currentIndex = gameState.getJourneyState().getCurrentLocationIndex();
        int totalLocations = gameState.getJourneyState().getLocations().size() - 1;
        if (totalLocations <= 0) return 0;
        double progress = (double) currentIndex / totalLocations;
        return (int) (progress * MAX_JOURNEY_BONUS);
    }

    private static int statScore(GameState gameState) {
        double health = gameState.getTeamState().getHealth() * HEALTH_WEIGHT;
        double energy = gameState.getTeamState().getEnergy() * ENERGY_WEIGHT;
        double morale = gameState.getTeamState().getMorale() * MORALE_WEIGHT;
        return (int) (health + energy + morale);
    }

    private static int resourceScore(GameState gameState) {
        double cash = normalize(gameState.getResourceState().getCash(), CASH_CAP) * 100 * CASH_WEIGHT;
        double food = normalize(gameState.getResourceState().getFood(), FOOD_CAP) * 100 * FOOD_WEIGHT;
        double compute = normalize(gameState.getResourceState().getComputeCredits(), COMPUTE_CAP) * 100 * COMPUTE_WEIGHT;
        return (int) (cash + food + compute);
    }

    private static double normalize(double value, double cap) {
        return Math.min(value / cap, 1.0);
    }
}
