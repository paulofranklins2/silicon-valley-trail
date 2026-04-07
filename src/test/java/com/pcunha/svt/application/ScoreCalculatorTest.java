package com.pcunha.svt.application;

import com.pcunha.svt.domain.model.*;
import com.pcunha.svt.infrastructure.data.GameDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreCalculatorTest {

    private ScoreCalculator scoreCalculator;

    @BeforeEach
    void setUp() {
        // use the real scoring.yaml so tests exercise the same formula as production.
        scoreCalculator = new ScoreCalculator(GameDataLoader.loadScoring());
    }

    private GameState createGameState(int health, int energy, int morale,
                                      int cash, int food, int compute,
                                      int locationIndex, double distanceToNext) {
        TeamState team = new TeamState(health, energy, morale);
        ResourceState resources = new ResourceState(cash, food, compute);
        // 15 locations, 14 distances
        List<Location> locations = List.of(
                new Location("A", 0, 0), new Location("B", 0, 0),
                new Location("C", 0, 0), new Location("D", 0, 0),
                new Location("E", 0, 0), new Location("F", 0, 0),
                new Location("G", 0, 0), new Location("H", 0, 0),
                new Location("I", 0, 0), new Location("J", 0, 0),
                new Location("K", 0, 0), new Location("L", 0, 0),
                new Location("M", 0, 0), new Location("N", 0, 0),
                new Location("O", 0, 0)
        );
        List<Double> distances = List.of(5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0);
        JourneyState journey = new JourneyState(locations, distances);
        // travel to reach the desired location index
        for (int i = 0; i < locationIndex; i++) {
            journey.travel(5.0);
        }
        return new GameState(team, resources, journey, "Test Team");
    }

    @Test
    void victoryWithFullStatsScoresHigh() {
        GameState gameState = createGameState(100, 100, 100, 200, 15, 15, 14, 0);
        gameState.getEndingState().markVictory();
        // only 10 turns - fast win
        int score = scoreCalculator.calculate(gameState);

        // victory(1000) + turn bonus(~333) + stats(600) + resources(600) = ~2500+
        assertTrue(score > 2000, "Perfect victory should score above 2000, got: " + score);
    }

    @Test
    void defeatHalfwayScoresMedium() {
        GameState gameState = createGameState(50, 50, 50, 50, 5, 5, 7, 2.5);
        gameState.getEndingState().setGameOver(true);
        int score = scoreCalculator.calculate(gameState);

        // journey(150) + stats(300) + resources(~250) = ~700
        assertTrue(score > 300, "Halfway defeat should score above 300, got: " + score);
        assertTrue(score < 1000, "Defeat should never reach victory-level scores, got: " + score);
    }

    @Test
    void earlyDeathScoresLow() {
        GameState gameState = createGameState(0, 0, 0, 0, 0, 0, 0, 5.0);
        gameState.getEndingState().setGameOver(true);
        int score = scoreCalculator.calculate(gameState);

        // journey(0) + stats(0) + resources(0) = 0
        assertEquals(0, score, "Early death with zero stats should score 0");
    }

    @Test
    void scoreIsNeverNegative() {
        GameState gameState = createGameState(0, 0, 0, 0, 0, 0, 0, 5.0);
        gameState.getEndingState().setGameOver(true);
        int score = scoreCalculator.calculate(gameState);

        assertTrue(score >= 0, "Score should never be negative");
    }

    @Test
    void fasterVictoryScoresHigherThanSlowVictory() {
        GameState fast = createGameState(80, 80, 80, 100, 10, 10, 14, 0);
        fast.getEndingState().markVictory();
        // simulate 15 turns
        for (int i = 0; i < 14; i++) fast.getProgressState().nextTurn();

        GameState slow = createGameState(80, 80, 80, 100, 10, 10, 14, 0);
        slow.getEndingState().markVictory();
        // simulate 29 turns
        for (int i = 0; i < 28; i++) slow.getProgressState().nextTurn();

        int fastScore = scoreCalculator.calculate(fast);
        int slowScore = scoreCalculator.calculate(slow);

        assertTrue(fastScore > slowScore,
                "Fast victory (" + fastScore + ") should outscore slow victory (" + slowScore + ")");
    }

    @Test
    void calculateFromGameStateAndLeaderboardEntryAgree() {
        GameState gameState = createGameState(85, 40, 70, 120, 6, 8, 14, 0);
        gameState.getEndingState().markVictory();

        // copy the same score inputs into an entry
        LeaderboardEntry entry = snapshotToEntry(gameState);

        int fromGameState = scoreCalculator.calculate(gameState);
        int fromEntry = scoreCalculator.calculate(entry);

        assertEquals(fromGameState, fromEntry,
                "Score must match for GameState and LeaderboardEntry");
    }

    @Test
    void calculateFromDefeatedGameAndEntryAgree() {
        GameState gameState = createGameState(0, 20, 30, 10, 2, 1, 7, 0);
        gameState.getEndingState().setGameOver(true);

        // copy the same score inputs into an entry
        LeaderboardEntry entry = snapshotToEntry(gameState);

        assertEquals(
                scoreCalculator.calculate(gameState),
                scoreCalculator.calculate(entry),
                "Defeat path must match for GameState and LeaderboardEntry"
        );
    }


    // copies score-relevant fields from GameState into a LeaderboardEntry.
    private static LeaderboardEntry snapshotToEntry(GameState gameState) {
        LeaderboardEntry entry = new LeaderboardEntry();
        entry.setVictory(gameState.getEndingState().isVictory());
        entry.setTurns(gameState.getProgressState().getTurn());
        entry.setLocationIndex(gameState.getJourneyState().getCurrentLocationIndex());
        entry.setTotalLocations(gameState.getJourneyState().getLocations().size());
        entry.setHealth(gameState.getTeamState().getHealth());
        entry.setEnergy(gameState.getTeamState().getEnergy());
        entry.setMorale(gameState.getTeamState().getMorale());
        entry.setCash(gameState.getResourceState().getCash());
        entry.setFood(gameState.getResourceState().getFood());
        entry.setComputeCredits(gameState.getResourceState().getComputeCredits());
        return entry;
    }
}
