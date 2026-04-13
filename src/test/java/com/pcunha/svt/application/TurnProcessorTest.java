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
        return createGameState(20.0);
    }

    private GameState createGameState(double distance) {
        Mockito.when(mockRandom.nextInt(Mockito.anyInt())).thenReturn(1);
        Mockito.when(mockWeatherPort.getWeather(Mockito.any()))
                .thenReturn(new WeatherSignal(WeatherCategory.CLEAR, 20.0));
        TeamState team = new TeamState(100, 100, 100);
        // Start with 20 compute so the travel speed penalty (kicks in at 0)
        // does not interfere with tests that just check turn flow.
        ResourceState resources = new ResourceState(100, 10, 20);
        JourneyState journey = new JourneyState(
                List.of(
                        new Location("A", 0, 0),
                        new Location("B", 0, 0)),
                List.of(distance)
        );
        return new GameState(team, resources, journey, "Test Team Name");
    }

    private GameState createThreeCityGameState() {
        Mockito.when(mockRandom.nextInt(Mockito.anyInt())).thenReturn(1);
        Mockito.when(mockWeatherPort.getWeather(Mockito.any()))
                .thenReturn(new WeatherSignal(WeatherCategory.CLEAR, 20.0));
        TeamState team = new TeamState(100, 100, 100);
        ResourceState resources = new ResourceState(100, 10, 20);
        JourneyState journey = new JourneyState(
                List.of(
                        new Location("A", 0, 0),
                        new Location("B", 0, 0),
                        new Location("C", 0, 0)),
                List.of(3.0, 20.0)
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
    public void endTimeIsStampedWhenGameEnds() {
        GameState gameState = createGameState();
        gameState.getProgressState().setStartTimeMs(System.currentTimeMillis() - 5000);
        gameState.getTeamState().changeHealth(-100);

        TurnProcessor turnProcessor = createProcessor();
        turnProcessor.processTurn(gameState, GameAction.TRAVEL);

        assertTrue(gameState.getEndingState().isGameOver());
        assertTrue(gameState.getProgressState().getEndTimeMs() > 0);
        long elapsed = gameState.getProgressState().getEndTimeMs()
                     - gameState.getProgressState().getStartTimeMs();
        assertTrue(elapsed >= 5000, "Elapsed time should be at least 5 seconds");
    }

    @Test
    public void travelTurnShouldCauseVictory() {
        // 12km journey at 3km/turn means 4 travels exactly reach the destination.
        GameState gameState = createGameState(12.0);
        TurnProcessor turnProcessor = createProcessor();

        turnProcessor.processTurn(gameState, GameAction.TRAVEL); // 3
        turnProcessor.processTurn(gameState, GameAction.TRAVEL); // 6
        turnProcessor.processTurn(gameState, GameAction.TRAVEL); // 9
        turnProcessor.processTurn(gameState, GameAction.TRAVEL); // 12 to arrived at B (destination)

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
        // 12km at 3km/turn means 4 travels exactly reach it.
        GameState gameState = createGameState(12.0);
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
        assertEquals(17, gameState.getJourneyState().getDistanceToNextLocation()); // 20 - 3
        assertEquals(95, gameState.getTeamState().getEnergy()); // 100 - 10 +5 weather
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