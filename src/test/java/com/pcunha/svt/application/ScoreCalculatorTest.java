package com.pcunha.svt.application;

import com.pcunha.svt.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreCalculatorTest {

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
        GameState gameState = new GameState(team, resources, journey, "Test Team");
        return gameState;
    }

    @Test
    void victoryWithFullStatsScoresHigh() {
        GameState gameState = createGameState(100, 100, 100, 200, 15, 15, 14, 0);
        gameState.setVictory(true);
        // only 10 turns — fast win
        int score = ScoreCalculator.calculate(gameState);

        // victory(1000) + turn bonus(~333) + stats(600) + resources(600) = ~2500+
        assertTrue(score > 2000, "Perfect victory should score above 2000, got: " + score);
    }

    @Test
    void defeatHalfwayScoresMedium() {
        GameState gameState = createGameState(50, 50, 50, 50, 5, 5, 7, 2.5);
        gameState.setGameOver(true);
        int score = ScoreCalculator.calculate(gameState);

        // journey(150) + stats(300) + resources(~250) = ~700
        assertTrue(score > 300, "Halfway defeat should score above 300, got: " + score);
        assertTrue(score < 1000, "Defeat should never reach victory-level scores, got: " + score);
    }

    @Test
    void earlyDeathScoresLow() {
        GameState gameState = createGameState(0, 0, 0, 0, 0, 0, 0, 5.0);
        gameState.setGameOver(true);
        int score = ScoreCalculator.calculate(gameState);

        // journey(0) + stats(0) + resources(0) = 0
        assertEquals(0, score, "Early death with zero stats should score 0");
    }

    @Test
    void scoreIsNeverNegative() {
        GameState gameState = createGameState(0, 0, 0, 0, 0, 0, 0, 5.0);
        gameState.setGameOver(true);
        int score = ScoreCalculator.calculate(gameState);

        assertTrue(score >= 0, "Score should never be negative");
    }

    @Test
    void fasterVictoryScoresHigherThanSlowVictory() {
        GameState fast = createGameState(80, 80, 80, 100, 10, 10, 14, 0);
        fast.setVictory(true);
        // simulate 15 turns
        for (int i = 0; i < 14; i++) fast.nextTurn();

        GameState slow = createGameState(80, 80, 80, 100, 10, 10, 14, 0);
        slow.setVictory(true);
        // simulate 29 turns
        for (int i = 0; i < 28; i++) slow.nextTurn();

        int fastScore = ScoreCalculator.calculate(fast);
        int slowScore = ScoreCalculator.calculate(slow);

        assertTrue(fastScore > slowScore,
                "Fast victory (" + fastScore + ") should outscore slow victory (" + slowScore + ")");
    }
}
