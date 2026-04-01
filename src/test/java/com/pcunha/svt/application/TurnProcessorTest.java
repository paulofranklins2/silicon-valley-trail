package com.pcunha.svt.application;

import com.pcunha.svt.domain.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TurnProcessorTest {
    private final Random mockRandom = Mockito.mock(Random.class);

    private GameState createGameState() {
        TeamState team = new TeamState(100, 100, 100);
        ResourceState resources = new ResourceState(100, 10, 5);
        JourneyState journey = new JourneyState(
                List.of(
                        new Location("A", 0, 0),
                        new Location("B", 0, 0)),
                List.of(20.0)
        );
        return new GameState(team, resources, journey);
    }

    @Test
    public void travelTurnGoesUpByOne() {
        GameState gameState = createGameState();
        TurnProcessor turnProcessor = new TurnProcessor(new ActionHandler(mockRandom), new ConditionEvaluator());
        turnProcessor.processTurn(gameState, GameAction.TRAVEL);
        turnProcessor.processTurn(gameState, GameAction.TRAVEL);
        assertEquals(3, gameState.getTurn());
    }

    @Test
    public void travelTurnCausesGameOver() {
        GameState gameState = createGameState();
        TurnProcessor turnProcessor = new TurnProcessor(new ActionHandler(mockRandom), new ConditionEvaluator());
        // team health is set to -100, should cause game over.
        gameState.getTeamState().changeHealth(-100);
        turnProcessor.processTurn(gameState, GameAction.TRAVEL);

        // turn counter
        assertEquals(1, gameState.getTurn());

        // game over should be true, and victory should be false.
        assertTrue(gameState.isGameOver());
        assertFalse(gameState.isVictory());
    }

    @Test
    public void travelTurnShouldCauseVictory() {
        // required 20
        GameState gameState = createGameState();
        TurnProcessor turnProcessor = new TurnProcessor(new ActionHandler(mockRandom), new ConditionEvaluator());
        turnProcessor.processTurn(gameState, GameAction.TRAVEL); // 5
        turnProcessor.processTurn(gameState, GameAction.TRAVEL); // 10
        turnProcessor.processTurn(gameState, GameAction.TRAVEL); // 15
        turnProcessor.processTurn(gameState, GameAction.TRAVEL); // 20

        // turn counter
        assertEquals(4, gameState.getTurn());

        // victory should be true, and game over should be true.
        assertTrue(gameState.isVictory());
        assertTrue(gameState.isGameOver());
    }

}