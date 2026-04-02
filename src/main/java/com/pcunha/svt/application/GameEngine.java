package com.pcunha.svt.application;

import com.pcunha.svt.domain.*;
import com.pcunha.svt.domain.port.DistancePort;

import java.util.ArrayList;
import java.util.List;

/**
 * Main game class. Sets up new games and runs turns.
 */
public class GameEngine {
    private final TurnProcessor turnProcessor;
    private final DistancePort distancePort;

    public GameEngine(TurnProcessor turnProcessor, DistancePort distancePort) {
        this.turnProcessor = turnProcessor;
        this.distancePort = distancePort;
    }

    /**
     * Sets up a new game with starting stats and real distances between locations.
     */
    public GameState createNewGame(String teamName) {
        TeamState teamState = new TeamState(100, 100, 100);
        ResourceState resourceState = new ResourceState(100, 5, 5);

        List<Location> locations = buildLocations();
        List<Double> distances = calculateDistances(locations, distancePort);

        JourneyState journeyState = new JourneyState(locations, distances);

        return new GameState(teamState, resourceState, journeyState, teamName);
    }

    /**
     * Runs one turn with the given action.
     */
    public void processAction(GameState gameState, GameAction gameAction) {
        turnProcessor.processTurn(gameState, gameAction);
    }

    // 10 real locations along the San Jose to San Francisco corridor
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

    // gets the distance between each location and the next one
    private List<Double> calculateDistances(List<Location> locations, DistancePort distancePort) {
        List<Double> distances = new ArrayList<>();
        for (int i = 0; i < locations.size() - 1; i++) {
            distances.add(distancePort.getDistance(locations.get(i), locations.get(i + 1)));
        }
        return distances;
    }

}
