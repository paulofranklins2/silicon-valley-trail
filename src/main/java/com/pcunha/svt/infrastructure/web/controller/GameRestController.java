package com.pcunha.svt.infrastructure.web.controller;

import com.pcunha.svt.application.GameEngine;
import com.pcunha.svt.application.LeaderboardService;
import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.model.ActionResponse;
import com.pcunha.svt.domain.model.ApiError;
import com.pcunha.svt.domain.model.GameEvent;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.MarketResult;
import com.pcunha.svt.domain.model.SubmissionResult;
import com.pcunha.svt.domain.model.TurnResult;
import com.pcunha.svt.infrastructure.web.exception.NoGameInSessionException;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class GameRestController {
    private final GameEngine gameEngine;
    private final LeaderboardService leaderboardService;

    public GameRestController(GameEngine gameEngine, LeaderboardService leaderboardService) {
        this.gameEngine = gameEngine;
        this.leaderboardService = leaderboardService;
    }

    @PostMapping("/retry-distances")
    public ResponseEntity<?> retryDistances(@RequestParam String gameMode, HttpSession session) {
        GameState existing = requireGame(session);

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

    @PostMapping("/action")
    public ResponseEntity<?> processActionApi(@RequestParam String action, HttpSession session) {
        GameState gameState = requireGame(session);

        TurnResult result;
        try {
            result = gameEngine.processAction(gameState, GameAction.valueOf(action));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError("Invalid action"));
        }
        return ResponseEntity.ok(new ActionResponse(gameState, result));
    }

    @PostMapping("/choice")
    public ResponseEntity<ActionResponse> processChoice(@RequestParam int choiceIndex, HttpSession session) {
        GameState gameState = requireGame(session);
        TurnResult result = gameEngine.resolveChoice(gameState, choiceIndex);
        return ResponseEntity.ok(new ActionResponse(gameState, result));
    }

    @GetMapping("/market")
    public Map<String, Object> getMarket(HttpSession session) {
        GameState gameState = requireGame(session);
        GameEvent market = gameEngine.getMarket(gameState);
        return Map.of("event", market, "purchased", gameState.getMarketState().getMarketPurchased());
    }

    @PostMapping("/market")
    public ResponseEntity<?> processMarketPurchase(@RequestParam int choiceIndex, HttpSession session) {
        GameState gameState = requireGame(session);

        MarketResult marketResult = gameEngine.buyFromMarket(gameState, choiceIndex);
        if (!marketResult.ok()) {
            return ResponseEntity.badRequest().body(new ApiError(marketResult.error()));
        }
        return ResponseEntity.ok(gameState);
    }

    @PostMapping("/leaderboard")
    public ResponseEntity<?> submitScore(@RequestParam String playerName, HttpSession session) {
        GameState gameState = requireGame(session);

        SubmissionResult result = leaderboardService.submitResult(gameState, playerName);
        if (result.ok()) {
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.badRequest().body(new ApiError(result.error()));
    }

    /**
     * Get game from session or throws if missing.
     */
    private GameState requireGame(HttpSession session) {
        GameState gameState = (GameState) session.getAttribute("gameState");
        if (gameState == null) throw new NoGameInSessionException();
        return gameState;
    }
}
