package com.pcunha.svt.application;

import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.model.*;
import com.pcunha.svt.domain.port.DistancePort;
import com.pcunha.svt.infrastructure.data.GameDataLoader;

import java.util.List;
import java.util.Map;

/**
 * Main game class. Sets up new games and runs turns.
 */
public class GameEngine {
    private final TurnProcessor turnProcessor;
    private final Map<GameMode, DistancePort> distancePorts;

    public GameEngine(TurnProcessor turnProcessor, Map<GameMode, DistancePort> distancePorts) {
        this.turnProcessor = turnProcessor;
        this.distancePorts = distancePorts;
    }

    /**
     * Sets up a new game with starting stats and real distances between locations.
     * Distance calculation depends on the selected game mode (Haversine or OSRM).
     */
    public GameState createNewGame(String teamName, GameMode gameMode) {
        TeamState teamState = new TeamState(100, 100, 100);
        ResourceState resourceState = new ResourceState(100, 5, 5);

        DistancePort distancePort = distancePorts.get(gameMode);
        List<Location> locations = GameDataLoader.loadLocations();
        DistanceResult result = distancePort.calculateLegDistances(locations);

        // if OSRM failed, downgrade to Fast mode so leaderboard ranking is fair
        GameMode effectiveMode = gameMode;
        if (result.usedFallback()) {
            effectiveMode = GameMode.FAST;
        }

        JourneyState journeyState = new JourneyState(locations, result.distances());

        GameState gameState = new GameState(teamState, resourceState, journeyState, teamName);
        gameState.setGameMode(effectiveMode);
        gameState.setUsedFallbackDistances(result.usedFallback());
        turnProcessor.fetchInitialWeather(gameState);
        return gameState;
    }

    /**
     * Runs one turn with the given action.
     */
    public void processAction(GameState gameState, GameAction gameAction) {
        turnProcessor.processTurn(gameState, gameAction);
    }

    /**
     * Resolves a player's choice for a pending event.
     */
    public void resolveChoice(GameState gameState, int choiceIndex) {
        turnProcessor.resolveChoice(gameState, choiceIndex);
    }

    /**
     * Returns a random city market event.
     */
    public GameEvent getCityMarketEvent() {
        return turnProcessor.getEventProcessor().getCityMarketEvent();
    }

    /**
     * Applies a market purchase outcome.
     * Returns false if the player can't afford it.
     */
    public boolean resolveMarketPurchase(GameState gameState, GameEvent marketEvent, int choiceIndex) {
        if (marketEvent == null || marketEvent.getOutcomes() == null) return false;
        if (choiceIndex < 0 || choiceIndex >= marketEvent.getOutcomes().size()) return false;

        var outcome = marketEvent.getOutcomes().get(choiceIndex);
        int cashCost = Math.abs(Math.min(0, outcome.getCashChange()));
        if (cashCost > 0 && gameState.getResourceState().getCash() < cashCost) {
            return false;
        }

        turnProcessor.getEventProcessor().applyOutcome(gameState, outcome);
        return true;
    }

}
