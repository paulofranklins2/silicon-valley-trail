package com.pcunha.svt.infrastructure.web.controller;

import com.pcunha.svt.application.GameEngine;
import com.pcunha.svt.application.LeaderboardService;
import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.model.GameEvent;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.MarketResult;
import com.pcunha.svt.domain.model.SubmissionResult;
import com.pcunha.svt.domain.port.LeaderboardPort;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class GameRestController {
    private final GameEngine gameEngine;
    private final LeaderboardPort leaderboardPort;
    private final LeaderboardService leaderboardService;


    public GameRestController(GameEngine gameEngine, LeaderboardPort leaderboardPort, LeaderboardService leaderboardService) {
        this.gameEngine = gameEngine;
        this.leaderboardPort = leaderboardPort;
        this.leaderboardService = leaderboardService;
    }

    @PostMapping("/api/retry-distances")
    public Object retryDistances(@RequestParam String gameMode, HttpSession session) {
        GameState existing = getGameState(session);
        if (existing == null) return Map.of("success", false, "error", "No game");

        GameMode mode = GameMode.valueOf(gameMode);
        boolean success = gameEngine.retryDistances(mode);

        if (success) {
            // recreate the game with the now-cached real distances
            GameState gameState = gameEngine.createNewGame(existing.getTeamName(), mode);
            session.setAttribute("gameState", gameState);
            return Map.of("success", true);
        }

        return Map.of("success", false, "error", "API still unavailable");
    }

    @PostMapping("/api/action")
    public Object processActionApi(@RequestParam String action, HttpSession session) {
        GameState gameState = getGameState(session);
        if (gameState == null) return "redirect:/";

        try {
            gameEngine.processAction(gameState, GameAction.valueOf(action));
        } catch (IllegalArgumentException e) {
            return Map.of("error", "Invalid action");
        }
        return gameState;
    }

    @PostMapping("/api/choice")
    public Object processChoice(@RequestParam int choiceIndex, HttpSession session) {
        GameState gameState = getGameState(session);
        if (gameState == null) {
            return "redirect:/";
        }
        gameEngine.resolveChoice(gameState, choiceIndex);
        return gameState;
    }

    @GetMapping("/api/market")
    public Object getMarket(HttpSession session) {
        GameState gameState = getGameState(session);
        if (gameState == null) return "redirect:/";

        GameEvent market = gameEngine.getMarket(gameState);
        return Map.of("event", market, "purchased", gameState.getMarketPurchased());
    }

    @PostMapping("/api/market")
    public Object processMarketPurchase(@RequestParam int choiceIndex, HttpSession session) {
        GameState gameState = getGameState(session);
        if (gameState == null) return "redirect:/";

        MarketResult marketResult = gameEngine.buyFromMarket(gameState, choiceIndex);
        if (!marketResult.ok()) {
            return Map.of("state", gameState, "purchased", gameState.getMarketPurchased(), "error", marketResult.error());
        }

        return Map.of("state", gameState, "purchased", gameState.getMarketPurchased());
    }

    @PostMapping("/api/leaderboard")
    public Object submitScore(@RequestParam String playerName, HttpSession session) {
        GameState gameState = getGameState(session);
        if (gameState == null) {
            return Map.of("error", "No game in progress");
        }

        SubmissionResult result = leaderboardService.submitResult(gameState, playerName);
        if (result.ok()) {
            return Map.of("success", true);
        } else {
            return Map.of("error", result.error());
        }
    }

    private GameState getGameState(HttpSession session) {
        return (GameState) session.getAttribute("gameState");
    }
}
