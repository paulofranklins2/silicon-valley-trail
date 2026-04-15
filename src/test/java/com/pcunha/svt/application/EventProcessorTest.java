package com.pcunha.svt.application;

import com.pcunha.svt.domain.*;
import com.pcunha.svt.domain.model.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
        new EventProcessor(mockRandom, Map.of(), List.of()).applyEvent(gameState, gameEvent);

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
        WeatherSignal weatherSignal = new WeatherSignal(WeatherCategory.CLEAR, 20.0);
        Mockito.when(mockRandom.nextInt(Mockito.anyInt())).thenReturn(0);
        GameEvent gameEvent = EventProcessor.create(mockRandom).generateEvent(weatherSignal, 1);

        assertNotNull(gameEvent);
        assertNotNull(gameEvent.getTitle());
    }

    @Test
    public void roughWeatherBiasesTowardWeatherCategory() {
        // force weather branch
        Mockito.when(mockRandom.nextDouble()).thenReturn(0.0);

        // pick first weather event
        Mockito.when(mockRandom.nextInt(Mockito.anyInt())).thenReturn(0);

        // generate event with rainy weather
        WeatherSignal rainy = new WeatherSignal(WeatherCategory.RAINY, 12.0);
        GameEvent event = EventProcessor.create(mockRandom).generateEvent(rainy, 1);

        // verify result
        assertNotNull(event);
        assertEquals(EventCategory.WEATHER, event.getEventCategory());
    }

    @Test
    public void clearWeatherMostlyPullsFromFullPool() {
        // skip weather branch
        Mockito.when(mockRandom.nextDouble()).thenReturn(0.99);

        // pick first event from full pool
        Mockito.when(mockRandom.nextInt(Mockito.anyInt())).thenReturn(0);

        // generate event with clear weather
        WeatherSignal clear = new WeatherSignal(WeatherCategory.CLEAR, 22.0);
        GameEvent event = EventProcessor.create(mockRandom).generateEvent(clear, 1);

        // verify result exists, full pull
        assertNotNull(event);
    }

    @Test
    public void bossCityIndexAlwaysPullsBossCategory() {
        Mockito.when(mockRandom.nextInt(Mockito.anyInt())).thenReturn(0);

        WeatherSignal clear = new WeatherSignal(WeatherCategory.CLEAR, 22.0);
        GameEvent event = EventProcessor.create(mockRandom).generateEvent(clear, 5);

        assertNotNull(event);
        assertEquals(EventCategory.BOSS, event.getEventCategory());
        assertNotNull(event.getOutcomes());
        assertEquals(5, event.getCityIndex());
        assertEquals(2, event.getOutcomes().size());
    }

    @Test
    public void nonBossCityNeverPullsBossCategoryFromFullPool() {
        Mockito.when(mockRandom.nextDouble()).thenReturn(0.99);
        Mockito.when(mockRandom.nextInt(Mockito.anyInt())).thenReturn(0);

        WeatherSignal clear = new WeatherSignal(WeatherCategory.CLEAR, 22.0);
        GameEvent event = EventProcessor.create(mockRandom).generateEvent(clear, 1);

        assertNotNull(event);
        assertNotEquals(EventCategory.BOSS, event.getEventCategory());
    }
}
