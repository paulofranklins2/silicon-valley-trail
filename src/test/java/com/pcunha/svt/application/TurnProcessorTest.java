package com.pcunha.svt.application;

import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.WeatherCategory;
import com.pcunha.svt.domain.model.*;
import com.pcunha.svt.domain.port.WeatherPort;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TurnProcessorTest {
    private final Random mockRandom = Mockito.mock(Random.class);
    private final WeatherPort mockWeatherPort = Mockito.mock(WeatherPort.class);

    private GameState createGameState() {
        Mockito.when(mockRandom.nextInt(Mockito.anyInt())).thenReturn(1);
        Mockito.when(mockWeatherPort.getWeather(Mockito.any()))
                .thenReturn(new WeatherSignal(WeatherCategory.CLEAR, 20.0));
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
    public void travelTurnGoesUpByOne() {
        Mockito.when(mockRandom.nextInt(Mockito.anyInt())).thenReturn(1);

        GameState gameState = createGameState();
        TurnProcessor turnProcessor = new TurnProcessor(
                ActionHandler.create(mockRandom), new ConditionEvaluator(),
                EventProcessor.create(mockRandom),
                mockRandom,
                mockWeatherPort
        );
        turnProcessor.processTurn(gameState, GameAction.TRAVEL);
        turnProcessor.processTurn(gameState, GameAction.TRAVEL);
        assertEquals(3, gameState.getProgressState().getTurn());
    }

    @Test
    public void travelTurnCausesGameOver() {
        GameState gameState = createGameState();
        // change team health to -100, should cause game over.
        gameState.getTeamState().changeHealth(-100);

        // override weather to RAINY after createGameState set it to CLEAR
        Mockito.when(mockWeatherPort.getWeather(Mockito.any()))
                .thenReturn(new WeatherSignal(WeatherCategory.RAINY, 15.0));

        TurnProcessor turnProcessor = new TurnProcessor(
                ActionHandler.create(mockRandom),
                new ConditionEvaluator(),
                EventProcessor.create(mockRandom),
                mockRandom,
                mockWeatherPort
        );
        turnProcessor.processTurn(gameState, GameAction.TRAVEL);

        // turn counter
        assertEquals(1, gameState.getProgressState().getTurn());

        // game over should be true, and victory should be false.
        assertTrue(gameState.getEndingState().isGameOver());
        assertFalse(gameState.getEndingState().isVictory());
    }

    @Test
    public void travelTurnShouldCauseVictory() {
        Mockito.when(mockRandom.nextInt(Mockito.anyInt())).thenReturn(1);

        // required 20
        GameState gameState = createGameState();
        TurnProcessor turnProcessor = new TurnProcessor(
                ActionHandler.create(mockRandom),
                new ConditionEvaluator(),
                EventProcessor.create(mockRandom),
                mockRandom,
                mockWeatherPort
        );
        turnProcessor.processTurn(gameState, GameAction.TRAVEL); // 5
        turnProcessor.processTurn(gameState, GameAction.TRAVEL); // 10
        turnProcessor.processTurn(gameState, GameAction.TRAVEL); // 15
        turnProcessor.processTurn(gameState, GameAction.TRAVEL); // 20

        // turn counter
        assertEquals(4, gameState.getProgressState().getTurn());

        // victory should be true, and game over should be true.
        assertTrue(gameState.getEndingState().isVictory());
        assertTrue(gameState.getEndingState().isGameOver());
    }

    @Test
    public void turnWithEventAppliesEvent() {
        // nextDouble returns 0.1, below EVENT_CHANCE (0.2), so event fires
        Mockito.when(mockRandom.nextDouble()).thenReturn(0.1);
        Mockito.when(mockRandom.nextInt(Mockito.anyInt())).thenReturn(1);

        GameState gameState = createGameState();
        TurnProcessor turnProcessor = new TurnProcessor(
                ActionHandler.create(mockRandom),
                new ConditionEvaluator(),
                EventProcessor.create(mockRandom),
                mockRandom,
                mockWeatherPort
        );
        turnProcessor.processTurn(gameState, GameAction.REST);

        // event should have fired
        assertNotNull(gameState.getProgressState().getLastTurnResult().getGameEvent());
    }

    @Test
    public void turnWithoutEventHasNullLastEvent() {
        // nextDouble returns 0.9, above EVENT_CHANCE (0.2), so no event
        Mockito.when(mockRandom.nextDouble()).thenReturn(0.9);

        GameState gameState = createGameState();
        TurnProcessor turnProcessor = new TurnProcessor(
                ActionHandler.create(mockRandom),
                new ConditionEvaluator(),
                EventProcessor.create(mockRandom),
                mockRandom,
                mockWeatherPort
        );
        turnProcessor.processTurn(gameState, GameAction.REST);

        // no event
        assertNull(gameState.getProgressState().getLastTurnResult().getGameEvent());
    }

    @Test
    public void actionStillAppliesWhenNoEvent() {
        // no event fires
        Mockito.when(mockRandom.nextDouble()).thenReturn(0.9);

        GameState gameState = createGameState();
        TurnProcessor turnProcessor = new TurnProcessor(
                ActionHandler.create(mockRandom),
                new ConditionEvaluator(),
                EventProcessor.create(mockRandom),
                mockRandom,
                mockWeatherPort
        );
        turnProcessor.processTurn(gameState, GameAction.TRAVEL);

        // action still applied even without event
        assertEquals(15, gameState.getJourneyState().getDistanceToNextLocation()); // 20 - 5
        assertEquals(90, gameState.getTeamState().getEnergy()); // 100 - 15 +5 weather
    }

    @Test
    public void weatherIsPassedToEventGenerator() {
        // trigger event
        Mockito.when(mockRandom.nextDouble()).thenReturn(0.1);
        Mockito.when(mockRandom.nextInt(Mockito.anyInt())).thenReturn(1);

        //  trigger weather change
        WeatherPort mockedWeatherPort = Mockito.mock(WeatherPort.class);
        Mockito.when(mockedWeatherPort.getWeather(Mockito.any()))
                .thenReturn(new WeatherSignal(WeatherCategory.RAINY, 15.0));

        GameState gameState = createGameState();
        TurnProcessor turnProcessor = new TurnProcessor(
                ActionHandler.create(mockRandom),
                new ConditionEvaluator(),
                EventProcessor.create(mockRandom),
                mockRandom,
                mockedWeatherPort
        );
        turnProcessor.processTurn(gameState, GameAction.TRAVEL);

        // verify weather was fetched for the current location
        Mockito.verify(mockedWeatherPort).getWeather(gameState.getJourneyState().getCurrentLocation());
    }

}