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
        // Start with 20 compute so travel does not trigger the
        // speed-penalty branch during these tests.
        ResourceState resources = new ResourceState(100, 10, 20);
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
        // health 100, energy 100, morale 100, cash 100, food 10, computeCredit 20
        GameState gameState = createGameState();
        ActionHandler handler = ActionHandler.create(mockRandom);
        // Travel has 2 outcomes (morale -5 OR health -5). Force the morale branch.
        when(mockRandom.nextInt(2)).thenReturn(0);

        // guaranteed: energy -10, food -1, computeCredit -2; outcome: morale -5
        handler.handle(gameState, GameAction.TRAVEL);

        assertEquals(0, gameState.getJourneyState().getCurrentLocationIndex());
        assertEquals(90, gameState.getTeamState().getEnergy());
        assertEquals(95, gameState.getTeamState().getMorale());
        assertEquals(100, gameState.getTeamState().getHealth());
        assertEquals(17, gameState.getJourneyState().getDistanceToNextLocation());
        assertEquals(9, gameState.getResourceState().getFood());
        assertEquals(18, gameState.getResourceState().getComputeCredits());

        // Now flip to the health branch.
        when(mockRandom.nextInt(2)).thenReturn(1);
        handler.handle(gameState, GameAction.TRAVEL);

        assertEquals(0, gameState.getJourneyState().getCurrentLocationIndex());
        assertEquals(80, gameState.getTeamState().getEnergy());
        assertEquals(95, gameState.getTeamState().getMorale());
        assertEquals(95, gameState.getTeamState().getHealth());
        assertEquals(14, gameState.getJourneyState().getDistanceToNextLocation());
        assertEquals(8, gameState.getResourceState().getFood());
        assertEquals(16, gameState.getResourceState().getComputeCredits());
    }

    @Test
    public void rest() {
        TeamState team = new TeamState(80, 70, 60);
        ResourceState resources = new ResourceState(100, 10, 20);
        JourneyState journey = new JourneyState(
                List.of(new Location("A", 0, 0), new Location("B", 0, 0)),
                List.of(20.0)
        );

        // health 80, energy 70, morale 60, cash 100, food 10, computeCredit 20
        GameState gameState = new GameState(team, resources, journey, "Test Team Name");
        ActionHandler actionHandler = ActionHandler.create(mockRandom);

        // health +8, energy +15, morale +8, food -1
        actionHandler.handle(gameState, GameAction.REST);

        assertEquals(0, gameState.getJourneyState().getCurrentLocationIndex());
        assertEquals(20, gameState.getJourneyState().getDistanceToNextLocation());

        assertEquals(88, gameState.getTeamState().getHealth());
        assertEquals(85, gameState.getTeamState().getEnergy());
        assertEquals(68, gameState.getTeamState().getMorale());
        assertEquals(9, gameState.getResourceState().getFood());
    }

    @Test
    public void scavenge() {
        // health 100, energy 100, morale 100, cash 100, food 10, computeCredit 20
        GameState gameState = createGameState();
        ActionHandler handler = ActionHandler.create(mockRandom);
        // Scavenge has 2 outcomes: food alone, or cash + morale loss. Pick food.
        when(mockRandom.nextInt(2)).thenReturn(0);
        handler.handle(gameState, GameAction.SCAVENGE);

        // guaranteed: energy -10, health -3; outcome: food +3
        assertEquals(0, gameState.getJourneyState().getCurrentLocationIndex());
        assertEquals(20, gameState.getJourneyState().getDistanceToNextLocation());
        assertEquals(90, gameState.getTeamState().getEnergy());
        assertEquals(97, gameState.getTeamState().getHealth());
        assertEquals(100, gameState.getTeamState().getMorale());
        assertEquals(13, gameState.getResourceState().getFood());

        // Now pick the cash + morale-hit branch.
        when(mockRandom.nextInt(2)).thenReturn(1);
        handler.handle(gameState, GameAction.SCAVENGE);

        // guaranteed again: energy -10, health -3; outcome: cash +15, morale -3
        assertEquals(80, gameState.getTeamState().getEnergy());
        assertEquals(94, gameState.getTeamState().getHealth());
        assertEquals(97, gameState.getTeamState().getMorale());
        assertEquals(115, gameState.getResourceState().getCash());
        assertEquals(13, gameState.getResourceState().getFood());
    }

    @Test
    public void hackathon() {
        // health 100, energy 100, morale 100, cash 100, food 10, computeCredit 20
        GameState gameState = createGameState();
        ActionHandler handler = ActionHandler.create(mockRandom);
        // Hackathon has 2 outcomes: jackpot (cash +15, food +1) or wreck (morale -8, health -8).
        // Pick jackpot.
        when(mockRandom.nextInt(2)).thenReturn(0);
        handler.handle(gameState, GameAction.HACKATHON);

        assertEquals(0, gameState.getJourneyState().getCurrentLocationIndex());
        assertEquals(20, gameState.getJourneyState().getDistanceToNextLocation());

        // guaranteed: compute +15, energy -12, food -1; jackpot: cash +15, food +1
        assertEquals(35, gameState.getResourceState().getComputeCredits());
        assertEquals(88, gameState.getTeamState().getEnergy());
        assertEquals(115, gameState.getResourceState().getCash());
        assertEquals(10, gameState.getResourceState().getFood());
        assertEquals(100, gameState.getTeamState().getMorale());
        assertEquals(100, gameState.getTeamState().getHealth());

        // Now flip to the wreck branch.
        when(mockRandom.nextInt(2)).thenReturn(1);
        handler.handle(gameState, GameAction.HACKATHON);

        // guaranteed adds again: compute +15, energy -12, food -1; wreck: morale -8, health -8
        assertEquals(50, gameState.getResourceState().getComputeCredits());
        assertEquals(76, gameState.getTeamState().getEnergy());
        assertEquals(115, gameState.getResourceState().getCash());
        assertEquals(9, gameState.getResourceState().getFood());
        assertEquals(92, gameState.getTeamState().getMorale());
        assertEquals(92, gameState.getTeamState().getHealth());
    }

    @Test
    public void pitchVcs() {
        // health 100, energy 100, morale 100, cash 100, food 10, computeCredit 20
        GameState gameState = createGameState();
        ActionHandler handler = ActionHandler.create(mockRandom);

        // Pitch VCs has 2 outcomes: cash +50 or (morale -8, health -3). Pick cash.
        when(mockRandom.nextInt(2)).thenReturn(0);
        handler.handle(gameState, GameAction.PITCH_VCS);

        assertEquals(0, gameState.getJourneyState().getCurrentLocationIndex());
        assertEquals(20, gameState.getJourneyState().getDistanceToNextLocation());

        // guaranteed: energy -5, compute -3, morale -2; outcome: cash +50
        assertEquals(150, gameState.getResourceState().getCash());
        assertEquals(98, gameState.getTeamState().getMorale());
        assertEquals(100, gameState.getTeamState().getHealth());

        // Now the failure branch.
        when(mockRandom.nextInt(2)).thenReturn(1);
        handler.handle(gameState, GameAction.PITCH_VCS);

        // guaranteed again: energy -5, compute -3, morale -2; outcome: morale -8, health -3
        assertEquals(150, gameState.getResourceState().getCash());
        assertEquals(88, gameState.getTeamState().getMorale());
        assertEquals(97, gameState.getTeamState().getHealth());
    }
}
