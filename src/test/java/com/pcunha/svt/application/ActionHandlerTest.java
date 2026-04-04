package com.pcunha.svt.application;

import com.pcunha.svt.domain.*;
import com.pcunha.svt.domain.model.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;


class ActionHandlerTest {
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
        return new GameState(team, resources, journey, "Test Team Name");
    }

    @Test
    public void travel() {
        // health 100, energy 100, morale 100, cash 100, food 10, computeCredit 5
        GameState gameState = createGameState();
        ActionHandler handler = ActionHandler.create(mockRandom);
        // energy -15, food -1, computeCredit -1
        handler.handle(gameState, GameAction.TRAVEL);

        // health 100, energy 85, morale 100, cash 100, food 9, computeCredit 4, still at index 0
        assertEquals(0, gameState.getJourneyState().getCurrentLocationIndex());
        assertEquals(85, gameState.getTeamState().getEnergy());
        assertEquals(15, gameState.getJourneyState().getDistanceToNextLocation());
        assertEquals(9, gameState.getResourceState().getFood());
        assertEquals(4, gameState.getResourceState().getComputeCredits());

        // health 100, energy 85, morale 100, cash 100, food 9, computeCredit 4
        // energy -15, food -1, computeCredit -1
        handler.handle(gameState, GameAction.TRAVEL);
        assertEquals(0, gameState.getJourneyState().getCurrentLocationIndex());
        assertEquals(70, gameState.getTeamState().getEnergy());
        assertEquals(10, gameState.getJourneyState().getDistanceToNextLocation());
        assertEquals(8, gameState.getResourceState().getFood());
        assertEquals(3, gameState.getResourceState().getComputeCredits());
    }

    @Test
    public void rest() {
        TeamState team = new TeamState(80, 70, 60);
        ResourceState resources = new ResourceState(100, 10, 5);
        JourneyState journey = new JourneyState(
                List.of(new Location("A", 0, 0), new Location("B", 0, 0)),
                List.of(20.0)
        );

        // health 80, energy 70, morale 60, cash 100, food 10, computeCredit 5
        GameState gameState = new GameState(team, resources, journey, "Test Team Name");
        ActionHandler actionHandler = ActionHandler.create(mockRandom);

        // health + 5, energy + 10, morale + 10, food - 1
        actionHandler.handle(gameState, GameAction.REST);

        // should not change location index, distance should still say 20
        assertEquals(0, gameState.getJourneyState().getCurrentLocationIndex());
        assertEquals(20, gameState.getJourneyState().getDistanceToNextLocation());

        // health 85, energy 80, morale 70, cash 100, food 9, computeCredit 5
        assertEquals(85, gameState.getTeamState().getHealth());
        assertEquals(80, gameState.getTeamState().getEnergy());
        assertEquals(70, gameState.getTeamState().getMorale());
        assertEquals(9, gameState.getResourceState().getFood());

    }

    @Test
    public void scavenge() {
        // health 100, energy 100, morale 100, cash 100, food 10, computeCredit 5
        GameState gameState = createGameState();
        ActionHandler handler = ActionHandler.create(mockRandom);
        // energy -10, food +2 or cash + 10
        when(mockRandom.nextBoolean()).thenReturn(true);
        handler.handle(gameState, GameAction.SCAVENGE);

        // should not change location index, distance should still say 20
        assertEquals(0, gameState.getJourneyState().getCurrentLocationIndex());
        assertEquals(20, gameState.getJourneyState().getDistanceToNextLocation());

        // energy -10, food +2
        assertEquals(90, gameState.getTeamState().getEnergy());
        assertEquals(12, gameState.getResourceState().getFood());

        // energy -10, food +2 or cash + 10
        when(mockRandom.nextBoolean()).thenReturn(false);
        handler.handle(gameState, GameAction.SCAVENGE);

        // energy -10, cash +10
        assertEquals(80, gameState.getTeamState().getEnergy());
        assertEquals(110, gameState.getResourceState().getCash());
    }

    @Test
    public void hackathon() {
        // health 100, energy 100, morale 100, cash 100, food 10, computeCredit 5
        GameState gameState = createGameState();
        ActionHandler handler = ActionHandler.create(mockRandom);
        // compute credit + 10, energy -5, morale -5, food -1
        handler.handle(gameState, GameAction.HACKATHON);

        // should not change location index, distance should still say 20
        assertEquals(0, gameState.getJourneyState().getCurrentLocationIndex());
        assertEquals(20, gameState.getJourneyState().getDistanceToNextLocation());

        // compute credit + 10, energy -15, morale -8, food -1
        assertEquals(15, gameState.getResourceState().getComputeCredits());
        assertEquals(85, gameState.getTeamState().getEnergy());
        assertEquals(92, gameState.getTeamState().getMorale());
        assertEquals(9, gameState.getResourceState().getFood());
    }

    @Test
    public void pitchVcs() {
        // health 100, energy 100, morale 100, cash 100, food 10, computeCredit 5
        GameState gameState = createGameState();
        ActionHandler handler = ActionHandler.create(mockRandom);

        when(mockRandom.nextBoolean()).thenReturn(true);
        handler.handle(gameState, GameAction.PITCH_VCS);

        // should not change location index, distance should still say 20
        assertEquals(0, gameState.getJourneyState().getCurrentLocationIndex());
        assertEquals(20, gameState.getJourneyState().getDistanceToNextLocation());

        // cash + 50, morale should not change
        assertEquals(150, gameState.getResourceState().getCash());
        assertEquals(100, gameState.getTeamState().getMorale());

        // cash should not change, morale -10
        when(mockRandom.nextBoolean()).thenReturn(false);
        handler.handle(gameState, GameAction.PITCH_VCS);
        assertEquals(150, gameState.getResourceState().getCash());
        assertEquals(90, gameState.getTeamState().getMorale());
    }
}