package com.pcunha.svt.application;

import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.model.*;
import com.pcunha.svt.domain.port.DistancePort;
import com.pcunha.svt.infrastructure.data.GameDataLoader;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Main game class. Sets up new games and runs turns.
 * Distances are pre-computed at startup and cached per game mode.
 */
public class GameEngine {
    private final TurnProcessor turnProcessor;
    private final List<Location> locations;
    private final Map<GameMode, DistanceResult> cachedDistances;
    private final Map<GameMode, DistancePort> distancePorts;

    public GameEngine(TurnProcessor turnProcessor, Map<GameMode, DistancePort> distancePorts) {
        this.turnProcessor = turnProcessor;
        this.distancePorts = distancePorts;
        this.locations = GameDataLoader.loadLocations();
        this.cachedDistances = new EnumMap<>(GameMode.class);
        preComputeDistances();
    }

    /**
     * Pre-compute distances for all available modes at startup.
     */
    private void preComputeDistances() {
        System.out.println("Computing distances...");

        // cache per port instance to avoid duplicate API calls for shared adapters
        Map<DistancePort, DistanceResult> portCache = new java.util.IdentityHashMap<>();

        for (Map.Entry<GameMode, DistancePort> entry : distancePorts.entrySet()) {
            GameMode mode = entry.getKey();
            DistancePort port = entry.getValue();

            DistanceResult result = portCache.get(port);
            if (result == null) {
                result = port.calculateLegDistances(locations);
                portCache.put(port, result);
            }

            cachedDistances.put(mode, result);

            if (result.usedFallback()) {
                System.out.println(mode.getDisplayName() + " mode: unavailable");
            } else {
                System.out.println(mode.getDisplayName() + " mode: ready");
            }
        }
    }

    /**
     * Set up a new game with pre-computed distances.
     * If the cached result used a fallback, downgrade to Fast mode.
     */
    public GameState createNewGame(String teamName, GameMode gameMode) {
        TeamState teamState = new TeamState(100, 100, 100);
        ResourceState resourceState = new ResourceState(100, 5, 5);

        DistanceResult result = cachedDistances.get(gameMode);

        GameMode effectiveMode = gameMode;
        if (result.usedFallback()) {
            effectiveMode = getFallbackMode(gameMode);
        }

        JourneyState journeyState = new JourneyState(locations, result.distances());

        GameState gameState = new GameState(teamState, resourceState, journeyState, teamName);
        ConfigState configState = gameState.getConfigState();
        configState.setGameMode(effectiveMode);
        configState.setRequestedGameMode(gameMode);
        configState.setUsedFallbackDistances(result.usedFallback());
        turnProcessor.loadInitialWeather(gameState);
        return gameState;
    }

    /**
     * Retry fetching distances for a mode that failed at startup.
     * Updates the cache if successful.
     */
    public boolean retryDistances(GameMode gameMode) {
        DistancePort port = distancePorts.get(gameMode);
        if (port == null) return false;

        DistanceResult result = port.calculateLegDistances(locations);
        cachedDistances.put(gameMode, result);
        return !result.usedFallback();
    }

    /**
     * Map mode to its fallback when api not avaliable.
     */
    private GameMode getFallbackMode(GameMode mode) {
        return switch (mode) {
            case MEDIUM -> GameMode.EASY;
            case IMPOSSIBLE -> GameMode.HARD;
            default -> GameMode.EASY;
        };
    }

    /**
     * Run one turn with the given action.
     */
    public TurnResult processAction(GameState gameState, GameAction gameAction) {
        return turnProcessor.processTurn(gameState, gameAction);
    }

    /**
     * Resolves a player's choice for a pending event.
     */
    public TurnResult resolveChoice(GameState gameState, int choiceIndex) {
        return turnProcessor.resolveChoice(gameState, choiceIndex);
    }

    /**
     * Returns the current city's market. Generates a new one if the player
     * moved to a new city or hasn't opened the market yet.
     */
    public GameEvent getMarket(GameState gameState) {
        int cityIndex = gameState.getJourneyState().getCurrentLocationIndex();
        MarketState marketState = gameState.getMarketState();

        if (marketState.getCurrentMarketEvent() == null || marketState.getMarketCityIndex() != cityIndex) {
            marketState.resetMarket();
            marketState.setCurrentMarketEvent(turnProcessor.getEventProcessor().getCityMarketEvent());
            marketState.setMarketCityIndex(cityIndex);
        }

        return marketState.getCurrentMarketEvent();
    }

    /**
     * Attempts a market purchase. Returns a result with success/error status.
     */
    public MarketResult buyFromMarket(GameState gameState, int choiceIndex) {
        MarketState marketState = gameState.getMarketState();
        GameEvent market = marketState.getCurrentMarketEvent();
        if (market == null || market.getOutcomes() == null) {
            return MarketResult.error("No market available");
        }
        if (choiceIndex < 0 || choiceIndex >= market.getOutcomes().size()) {
            return MarketResult.error("Invalid choice");
        }

        boolean isSkip = choiceIndex == market.getOutcomes().size() - 1;

        if (!isSkip && marketState.getMarketPurchased().contains(choiceIndex)) {
            return MarketResult.error("Already purchased");
        }

        var outcome = market.getOutcomes().get(choiceIndex);
        int cashCost = Math.abs(Math.min(0, outcome.getCashChange()));
        if (cashCost > 0 && gameState.getResourceState().getCash() < cashCost) {
            return MarketResult.error("Not enough cash");
        }

        turnProcessor.getEventProcessor().applyOutcome(gameState, outcome);

        if (!isSkip) {
            marketState.addMarketPurchase(choiceIndex);
        }

        // reset grace counters if player recovered resources via market
        ResourceGraceState graceState = gameState.getGraceState();
        if (gameState.getResourceState().getFood() > 0) {
            graceState.resetTurnWithoutFood();
        }
        if (gameState.getResourceState().getCash() > 0) {
            graceState.resetTurnWithoutCash();
        }

        return MarketResult.success();
    }
}
