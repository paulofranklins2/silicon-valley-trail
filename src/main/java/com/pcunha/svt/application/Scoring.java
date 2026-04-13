package com.pcunha.svt.application;

/**
 * Scoring parameters used by ScoreCalculator.
 * Loaded from configuration and shared across all runs.
 */
public record Scoring(
        int victoryBonus,
        int maxTurnBonus,
        int maxTimeBonus,
        int maxJourneyBonus,
        int turnBonusBaseline,
        int timeBonusBaselineSeconds,
        StatWeights statWeights,
        ResourceWeights resourceWeights,
        ResourceCaps resourceCaps) {
    public record StatWeights(double health, double energy, double morale) {
    }

    public record ResourceWeights(double cash, double food, double compute) {
    }

    public record ResourceCaps(double cash, double food, double compute) {
    }
}
