package com.pcunha.svt.application;

import com.pcunha.svt.domain.*;

import java.util.List;

public class GameEngine {
    private final TurnProcessor turnProcessor;

    public GameEngine(TurnProcessor turnProcessor) {
        this.turnProcessor = turnProcessor;
    }

    public GameState createNewGame(String teamName) {
        TeamState teamState = new TeamState(100, 100, 100);
        ResourceState resourceState = new ResourceState(100, 5, 5);
        JourneyState journeyState = new JourneyState(buildLocations(), buildDistance());

        return new GameState(teamState, resourceState, journeyState);
    }

    public void processAction(GameState gameState, GameAction gameAction) {
        turnProcessor.processTurn(gameState, gameAction);
    }

    private List<Location> buildLocations() {
        return List.of(
                new Location("San Jose", 37.3382, -121.8863),
                new Location("Santa Clara", 37.3541, -121.9552),
                new Location("Sunnyvale", 37.3688, -122.0363),
                new Location("Mountain View", 37.3861, -122.0839),
                new Location("Palo Alto", 37.4419, -122.1430),
                new Location("Menlo Park", 37.4530, -122.1817),
                new Location("Redwood City", 37.4852, -122.2364),
                new Location("San Carlos", 37.5072, -122.2605),
                new Location("South San Francisco", 37.6547, -122.4077),
                new Location("San Francisco", 37.7749, -122.4194)
        );
    }

    private List<Double> buildDistance() {
        return List.of(8.0, 6.0, 5.0, 6.0, 5.0, 8.0, 5.0, 15.0, 13.0);
    }

}
