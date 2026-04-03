package com.pcunha.svt.application;

import com.pcunha.svt.domain.*;
import com.pcunha.svt.domain.model.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EventProcessorTest {
    private final Random mockRandom = Mockito.mock(Random.class);

    private GameState createGameState() {
        TeamState team = new TeamState(80, 80, 80);
        ResourceState resources = new ResourceState(100, 10, 5);
        JourneyState journey = new JourneyState(
                List.of(
                        new Location("A", 0, 0),
                        new Location("B", 0, 0)),
                List.of(20.0)
        );
        return new GameState(team, resources, journey, "Test Team Name");
    }

    private GameEvent createGameEvent() {
        return GameEvent.builder()
                .title("Test Event")
                .healthChange(-10)
                .energyChange(-5)
                .moraleChange(5)
                .cashChange(20)
                .foodChange(-1)
                .computeCreditsChange(3)
                .build();
    }

    @Test
    public void applyEventChangesState() {
        // health 80, energy 80, morale 80, cash 100, food 10, compute credits 5
        GameState gameState = createGameState();
        // health -10, energy -5, morale 5, cash 20, food -1, compute credits 3
        GameEvent gameEvent = createGameEvent();
        new EventProcessor(mockRandom).applyEvent(gameState, gameEvent);

        // health 70, energy 75, morale 85, cash 120, food 9, compute credits 8
        assertEquals(70, gameState.getTeamState().getHealth());
        assertEquals(75, gameState.getTeamState().getEnergy());
        assertEquals(85, gameState.getTeamState().getMorale());
        assertEquals(120, gameState.getResourceState().getCash());
        assertEquals(9, gameState.getResourceState().getFood());
        assertEquals(8, gameState.getResourceState().getComputeCredits());
    }

    @Test
    public void generateEventReturnsValidEvent() {
        GameState gameState = createGameState();
        WeatherSignal weatherSignal = new WeatherSignal(WeatherCategory.CLEAR, 20.0);
        Mockito.when(mockRandom.nextInt(Mockito.anyInt())).thenReturn(0);
        GameEvent gameEvent = new EventProcessor(mockRandom).generateEvent(gameState, weatherSignal);

        assertNotNull(gameEvent);
        assertNotNull(gameEvent.getTitle());
    }


}