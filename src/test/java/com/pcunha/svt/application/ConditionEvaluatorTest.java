package com.pcunha.svt.application;

import com.pcunha.svt.domain.LossReason;
import com.pcunha.svt.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConditionEvaluatorTest {

    private GameState createGameState() {
        TeamState team = new TeamState(100, 100, 100);
        ResourceState resources = new ResourceState(100, 10, 5);
        JourneyState journey = new JourneyState(List.of(new Location("A", 0, 0), new Location("B", 0, 0)), List.of(20.0));
        return new GameState(team, resources, journey, "Test Team Name");
    }

    @Test
    void victoryWhenDestinationReached() {
        // health 100, energy 100, morale 100, cash 100, food 10, compute credits 5
        ConditionEvaluator evaluator = new ConditionEvaluator();
        GameState gameState = createGameState();

        // travel 20km
        gameState.getJourneyState().travel(20);
        // should be at index 1
        assertEquals(1, gameState.getJourneyState().getCurrentLocationIndex());
        // distance for next dest should be zero
        assertEquals(0, gameState.getJourneyState().getDistanceToNextLocation());

        // evaluate game state
        evaluator.evaluate(gameState);
        // should be a victory
        assertTrue(gameState.isVictory());
    }

    @Test
    public void gameOverWhenHealthIsZero() {
        // health 100, energy 100, morale 100, cash 100, food 10, compute credits 5
        ConditionEvaluator evaluator = new ConditionEvaluator();
        GameState gameState = createGameState();

        // set health to -100
        gameState.getTeamState().changeHealth(-100);

        // evaluate game state
        evaluator.evaluate(gameState);
        // should be a game over
        assertTrue(gameState.isGameOver());
        // should not be a victory
        assertFalse(gameState.isVictory());
    }

    @Test
    public void gameOverWhenMoraleIsZero() {
        // health 100, energy 100, morale 100, cash 100, food 10, compute credits 5
        ConditionEvaluator evaluator = new ConditionEvaluator();
        GameState gameState = createGameState();

        // set morale to -100
        gameState.getTeamState().changeMorale(-100);

        // evaluate game State
        evaluator.evaluate(gameState);
        // should be a game over
        assertTrue(gameState.isGameOver());
        // should not be a victory
        assertFalse(gameState.isVictory());
    }

    @Test
    public void victoryShouldTakePriorityOverZeroHealth() {
        // health 100, energy 100, morale 100, cash 100, food 10, compute credits 5
        ConditionEvaluator evaluator = new ConditionEvaluator();
        GameState gameState = createGameState();

        // travel 20km
        gameState.getJourneyState().travel(20);
        // set health to -100
        gameState.getTeamState().changeHealth(-100);

        // should be at index 1
        assertEquals(1, gameState.getJourneyState().getCurrentLocationIndex());
        // distance for next dest should be zero
        assertEquals(0, gameState.getJourneyState().getDistanceToNextLocation());

        // evaluate game state
        evaluator.evaluate(gameState);
        // should be a victory
        assertTrue(gameState.isVictory());
    }

    @Test
    public void noGameOverWhenStatsAreOk() {
        // health 100, energy 100, morale 100, cash 100, food 10, compute credits 5
        ConditionEvaluator evaluator = new ConditionEvaluator();
        GameState gameState = createGameState();

        // evaluate game state
        evaluator.evaluate(gameState);
        // should not be a victory
        assertFalse(gameState.isVictory());
        // should not be a game over
        assertFalse(gameState.isGameOver());
    }

    @Test
    public void gameOverWhenFoodDepletedForTwoTurns() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        GameState gameState = createGameState();

        // set food to 0
        gameState.getResourceState().changeFood(-10);

        // evaluate twice (2 turns with no food)
        evaluator.evaluate(gameState);
        evaluator.evaluate(gameState);

        // should be a game over due to starvation
        assertTrue(gameState.isGameOver());
        assertEquals(LossReason.STARVATION, gameState.getLossReason());
    }

    @Test
    public void noGameOverWhenFoodDepletedForOnlyOneTurn() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        GameState gameState = createGameState();

        // set food to 0
        gameState.getResourceState().changeFood(-10);

        // evaluate once (grace period not exceeded)
        evaluator.evaluate(gameState);

        // should not be a game over yet
        assertFalse(gameState.isGameOver());
    }

    @Test
    public void gameOverWhenCashDepletedForThreeTurns() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        GameState gameState = createGameState();

        // set cash to 0
        gameState.getResourceState().changeCash(-100);

        // evaluate three times (3 turns with no cash)
        evaluator.evaluate(gameState);
        evaluator.evaluate(gameState);
        evaluator.evaluate(gameState);

        // should be a game over due to no cash
        assertTrue(gameState.isGameOver());
        assertEquals(LossReason.NO_CASH, gameState.getLossReason());
    }

    @Test
    public void noGameOverWhenCashDepletedForTwoTurns() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        GameState gameState = createGameState();

        // set cash to 0
        gameState.getResourceState().changeCash(-100);

        // evaluate twice (need 3 turns for cash game over)
        evaluator.evaluate(gameState);
        evaluator.evaluate(gameState);

        // should not be a game over yet
        assertFalse(gameState.isGameOver());
    }

    @Test
    public void foodCounterResetsWhenFoodRecovered() {
        ConditionEvaluator evaluator = new ConditionEvaluator();
        GameState gameState = createGameState();

        // set food to 0 and evaluate once (counter=1)
        gameState.getResourceState().changeFood(-10);
        evaluator.evaluate(gameState);

        // recover food (counter should reset)
        gameState.getResourceState().changeFood(5);
        evaluator.evaluate(gameState);

        // deplete food again and evaluate once (counter restarts at 1)
        gameState.getResourceState().changeFood(-5);
        evaluator.evaluate(gameState);

        // should not be a game over (counter is 1, not accumulated to 2)
        assertFalse(gameState.isGameOver());
    }
}