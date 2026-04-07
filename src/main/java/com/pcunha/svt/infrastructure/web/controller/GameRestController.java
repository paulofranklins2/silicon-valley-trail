package com.pcunha.svt.infrastructure.web.controller;

import com.pcunha.svt.application.GameEngine;
import com.pcunha.svt.application.LeaderboardService;
import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.model.GameEvent;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.MarketResult;
import com.pcunha.svt.domain.model.SubmissionResult;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class GameRestController {
    private static final Map<String, Object> NO_GAME_ERROR = Map.of("error", "No game in progress");

    private final GameEngine gameEngine;
    private final LeaderboardService leaderboardService;

    public GameRestController(GameEngine gameEngine, LeaderboardService leaderboardService) {
        this.gameEngine = gameEngine;
        this.leaderboardService = leaderboardService;
    }

    @PostMapping("/api/retry-distances")
    public ResponseEntity<?> retryDistances(@RequestParam String gameMode, HttpSession session) {
        GameState existing = getGameState(session);
        if (existing == null) return ResponseEntity.status(404).body(Map.of("success", false, "error", "No game"));

        GameMode mode = GameMode.valueOf(gameMode);
        boolean success = gameEngine.retryDistances(mode);

        if (success) {
            // recreate the game with the now-cached real distances
            GameState gameState = gameEngine.createNewGame(existing.getTeamName(), mode);
            session.setAttribute("gameState", gameState);
            return ResponseEntity.ok(Map.of("success", true));
        }

        return ResponseEntity.status(503).body(Map.of("success", false, "error", "API still unavailable"));
    }

    @PostMapping("/api/action")
    public ResponseEntity<?> processActionApi(@RequestParam String action, HttpSession session) {
        GameState gameState = getGameState(session);
        if (gameState == null) return ResponseEntity.status(404).body(NO_GAME_ERROR);

        try {
            gameEngine.processAction(gameState, GameAction.valueOf(action));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid action"));
        }
        return ResponseEntity.ok(gameState);
    }

    @PostMapping("/api/choice")
    public ResponseEntity<?> processChoice(@RequestParam int choiceIndex, HttpSession session) {
        GameState gameState = getGameState(session);
        if (gameState == null) return ResponseEntity.status(404).body(NO_GAME_ERROR);

        gameEngine.resolveChoice(gameState, choiceIndex);
        return ResponseEntity.ok(gameState);
    }

    @GetMapping("/api/market")
    public Object getMarket(HttpSession session) {
        GameState gameState = getGameState(session);
        if (gameState == null) return NO_GAME_ERROR;

        GameEvent market = gameEngine.getMarket(gameState);
        return Map.of("event", market, "purchased", gameState.getMarketState().getMarketPurchased());
    }

    @PostMapping("/api/market")
    public ResponseEntity<?> processMarketPurchase(@RequestParam int choiceIndex, HttpSession session) {
        GameState gameState = getGameState(session);
        if (gameState == null) return ResponseEntity.status(404).body(NO_GAME_ERROR);

        MarketResult marketResult = gameEngine.buyFromMarket(gameState, choiceIndex);
        if (!marketResult.ok()) {
            return ResponseEntity.badRequest().body(Map.of("error", marketResult.error()));
        }
        return ResponseEntity.ok(gameState);
    }

    @PostMapping("/api/leaderboard")
    public ResponseEntity<?> submitScore(@RequestParam String playerName, HttpSession session) {
        GameState gameState = getGameState(session);
        if (gameState == null) return ResponseEntity.status(404).body(NO_GAME_ERROR);

        SubmissionResult result = leaderboardService.submitResult(gameState, playerName);
        if (result.ok()) {
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.badRequest().body(Map.of("error", result.error()));
    }

    private GameState getGameState(HttpSession session) {
        return (GameState) session.getAttribute("gameState");
    }
}
