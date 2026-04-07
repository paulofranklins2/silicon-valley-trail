package com.pcunha.svt.application;

import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.WeatherCategory;
import com.pcunha.svt.domain.model.*;
import com.pcunha.svt.domain.port.WeatherPort;
import com.pcunha.svt.infrastructure.data.GameDataLoader;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TurnProcessorTest {
    private static final Tunables TUNABLES = GameDataLoader.loadTunables();
    private final Random mockRandom = Mockito.mock(Random.class);
    private final WeatherPort mockWeatherPort = Mockito.mock(WeatherPort.class);

    private TurnProcessor createProcessor() {
        return new TurnProcessor(
                ActionHandler.create(mockRandom),
                new ConditionEvaluator(TUNABLES),
                EventProcessor.create(mockRandom),
                mockWeatherPort,
                TUNABLES
        );
    }

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

    private GameState createThreeCityGameState() {
        Mockito.when(mockRandom.nextInt(Mockito.anyInt())).thenReturn(1);
        Mockito.when(mockWeatherPort.getWeather(Mockito.any()))
                .thenReturn(new WeatherSignal(WeatherCategory.CLEAR, 20.0));
        TeamState team = new TeamState(100, 100, 100);
        ResourceState resources = new ResourceState(100, 10, 5);
        JourneyState journey = new JourneyState(
                List.of(
                        new Location("A", 0, 0),
                        new Location("B", 0, 0),
                        new Location("C", 0, 0)),
                List.of(5.0, 20.0)
        );
        return new GameState(team, resources, journey, "Test Team Name");
    }

    @Test
    public void travelTurnGoesUpByOne() {
        GameState gameState = createGameState();
        TurnProcessor turnProcessor = createProcessor();

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

        TurnProcessor turnProcessor = createProcessor();
        turnProcessor.processTurn(gameState, GameAction.TRAVEL);

        // turn counter
        assertEquals(1, gameState.getProgressState().getTurn());

        // game over should be true, and victory should be false.
        assertTrue(gameState.getEndingState().isGameOver());
        assertFalse(gameState.getEndingState().isVictory());
    }

    @Test
    public void travelTurnShouldCauseVictory() {
        GameState gameState = createGameState();
        TurnProcessor turnProcessor = createProcessor();

        turnProcessor.processTurn(gameState, GameAction.TRAVEL); // 5
        turnProcessor.processTurn(gameState, GameAction.TRAVEL); // 10
        turnProcessor.processTurn(gameState, GameAction.TRAVEL); // 15
        turnProcessor.processTurn(gameState, GameAction.TRAVEL); // 20 to arrived at B (destination)

        // turn counter
        assertEquals(4, gameState.getProgressState().getTurn());

        // victory should be true, and game over should be true.
        assertTrue(gameState.getEndingState().isVictory());
        assertTrue(gameState.getEndingState().isGameOver());
    }

    @Test
    public void arrivingAtIntermediateCityTriggersEvent() {
        // Three-city journey: A to B is 5km (crossed in 1 travel), BtoC is 20km.
        GameState gameState = createThreeCityGameState();
        TurnProcessor turnProcessor = createProcessor();

        // TRAVEL 5km to cross from A to B (intermediate, not destination)
        TurnResult turnResult = turnProcessor.processTurn(gameState, GameAction.TRAVEL);

        // arrived at intermediate city to event should have fired
        assertNotNull(turnResult.getGameEvent());
        assertEquals(1, gameState.getJourneyState().getCurrentLocationIndex());
        assertFalse(gameState.getEndingState().isGameOver());
    }

    @Test
    public void nonArrivalTurnHasNoEvent() {
        // 20km between cities; a single 5km travel does not cross
        GameState gameState = createGameState();
        TurnProcessor turnProcessor = createProcessor();

        TurnResult turnResult = turnProcessor.processTurn(gameState, GameAction.TRAVEL);

        // still at city A, no event
        assertNull(turnResult.getGameEvent());
        assertEquals(0, gameState.getJourneyState().getCurrentLocationIndex());
    }

    @Test
    public void restActionNeverTriggersEvent() {
        GameState gameState = createGameState();
        TurnProcessor turnProcessor = createProcessor();

        TurnResult turnResult = turnProcessor.processTurn(gameState, GameAction.REST);

        // REST never advances location, so it never triggers an event
        assertNull(turnResult.getGameEvent());
    }

    @Test
    public void arrivingAtDestinationDoesNotTriggerEvent() {
        // 2-city journey where the second city IS the destination.
        // Arriving there should trigger victory, not an event.
        GameState gameState = createGameState();
        TurnProcessor turnProcessor = createProcessor();

        turnProcessor.processTurn(gameState, GameAction.TRAVEL);
        turnProcessor.processTurn(gameState, GameAction.TRAVEL);
        turnProcessor.processTurn(gameState, GameAction.TRAVEL);
        TurnResult turnResult = turnProcessor.processTurn(gameState, GameAction.TRAVEL);

        // victory, and no intermediate event on the final turn
        assertTrue(gameState.getEndingState().isVictory());
        assertNull(turnResult.getGameEvent());
    }

    @Test
    public void actionStillAppliesWhenNoEvent() {
        GameState gameState = createGameState();
        TurnProcessor turnProcessor = createProcessor();
        turnProcessor.processTurn(gameState, GameAction.TRAVEL);

        // action still applied even without event
        assertEquals(15, gameState.getJourneyState().getDistanceToNextLocation()); // 20 - 5
        assertEquals(90, gameState.getTeamState().getEnergy()); // 100 - 15 +5 weather
    }

    @Test
    public void weatherIsFetchedEveryTurn() {
        WeatherPort mockedWeatherPort = Mockito.mock(WeatherPort.class);
        Mockito.when(mockedWeatherPort.getWeather(Mockito.any()))
                .thenReturn(new WeatherSignal(WeatherCategory.RAINY, 15.0));

        GameState gameState = createGameState();
        TurnProcessor turnProcessor = new TurnProcessor(
                ActionHandler.create(mockRandom),
                new ConditionEvaluator(TUNABLES),
                EventProcessor.create(mockRandom),
                mockedWeatherPort,
                TUNABLES
        );
        turnProcessor.processTurn(gameState, GameAction.TRAVEL);

        // verify weather was fetched for the current location
        Mockito.verify(mockedWeatherPort).getWeather(gameState.getJourneyState().getCurrentLocation());
    }
}