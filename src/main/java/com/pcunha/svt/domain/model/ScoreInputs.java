package com.pcunha.svt.domain.model;

/**
 * Per-run inputs used by the scoring formula.
 * Built from GameState or LeaderboardEntry.
 */
public record ScoreInputs(
        boolean victory,
        int turns,
        long elapsedMs,
        int locationIndex,
        int totalLocations,
        int health,
        int energy,
        int morale,
        int cash,
        int food,
        int computeCredits) {
}