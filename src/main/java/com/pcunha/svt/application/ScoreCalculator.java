package com.pcunha.svt.application;

import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.LeaderboardEntry;
import com.pcunha.svt.domain.model.ScoreInputs;
import org.springframework.stereotype.Component;

/**
 * Calculates leaderboard scores.
 * Supports scoring from a live GameState or a persisted LeaderboardEntry.
 */
@Component
public class ScoreCalculator {
    private final Scoring scoring;

    public ScoreCalculator(Scoring scoring) {
        this.scoring = scoring;
    }

    public int calculate(GameState gameState) {
        return calculate(toInputs(gameState));
    }

    public int calculate(LeaderboardEntry entry) {
        return calculate(entry.toScoreInputs());
    }

    public int calculate(ScoreInputs inputs) {
        int score = 0;

        if (inputs.victory()) {
            score += scoring.victoryBonus();
            score += turnEfficiencyBonus(inputs.turns());
            score += timeEfficiencyBonus(inputs.elapsedMs());
        } else {
            score += journeyProgressBonus(inputs.locationIndex(), inputs.totalLocations());
        }

        score += statScore(inputs);
        score += resourceScore(inputs);

        return Math.max(0, score);
    }

    private static ScoreInputs toInputs(GameState gameState) {
        long start = gameState.getProgressState().getStartTimeMs();
        long end = gameState.getProgressState().getEndTimeMs();
        long elapsed = (start > 0 && end > start) ? end - start : 0;
        return new ScoreInputs(
                gameState.getEndingState().isVictory(),
                gameState.getProgressState().getTurn(),
                elapsed,
                gameState.getJourneyState().getCurrentLocationIndex(),
                gameState.getJourneyState().getLocations().size(),
                gameState.getTeamState().getHealth(),
                gameState.getTeamState().getEnergy(),
                gameState.getTeamState().getMorale(),
                gameState.getResourceState().getCash(),
                gameState.getResourceState().getFood(),
                gameState.getResourceState().getComputeCredits()
        );
    }

    private int turnEfficiencyBonus(int turns) {
        // fewer turns = higher bonus, zero once the player hits the baseline
        int baseline = scoring.turnBonusBaseline();
        if (turns >= baseline) return 0;
        double ratio = 1.0 - ((double) turns / baseline);
        return (int) (ratio * scoring.maxTurnBonus());
    }

    private int timeEfficiencyBonus(long elapsedMs) {
        if (elapsedMs <= 0) return 0;
        long elapsedSeconds = elapsedMs / 1000;
        int baseline = scoring.timeBonusBaselineSeconds();
        if (elapsedSeconds >= baseline) return 0;
        double ratio = 1.0 - ((double) elapsedSeconds / baseline);
        return (int) (ratio * scoring.maxTimeBonus());
    }

    private int journeyProgressBonus(int locationIndex, int totalLocations) {
        int denominator = totalLocations - 1;
        if (denominator <= 0) return 0;
        double progress = (double) locationIndex / denominator;
        return (int) (progress * scoring.maxJourneyBonus());
    }

    private int statScore(ScoreInputs inputs) {
        Scoring.StatWeights w = scoring.statWeights();
        double health = inputs.health() * w.health();
        double energy = inputs.energy() * w.energy();
        double morale = inputs.morale() * w.morale();
        return (int) (health + energy + morale);
    }

    private int resourceScore(ScoreInputs inputs) {
        Scoring.ResourceWeights w = scoring.resourceWeights();
        Scoring.ResourceCaps c = scoring.resourceCaps();
        double cash = normalize(inputs.cash(), c.cash()) * 100 * w.cash();
        double food = normalize(inputs.food(), c.food()) * 100 * w.food();
        double compute = normalize(inputs.computeCredits(), c.compute()) * 100 * w.compute();
        return (int) (cash + food + compute);
    }

    private static double normalize(double value, double cap) {
        return Math.min(value / cap, 1.0);
    }
}
