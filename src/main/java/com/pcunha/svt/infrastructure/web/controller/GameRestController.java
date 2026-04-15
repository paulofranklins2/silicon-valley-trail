package com.pcunha.svt.infrastructure.web.controller;

import com.pcunha.svt.application.GameEngine;
import com.pcunha.svt.application.LeaderboardService;
import com.pcunha.svt.application.RoomService;
import com.pcunha.svt.application.RoomService.LoadedSession;
import com.pcunha.svt.application.AuthService;
import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.model.*;
import com.pcunha.svt.infrastructure.web.PlayerCookies;
import com.pcunha.svt.infrastructure.web.exception.NoGameInSessionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class GameRestController {
    private final GameEngine gameEngine;
    private final RoomService roomService;
    private final LeaderboardService leaderboardService;
    private final AuthService authService;

    public GameRestController(GameEngine gameEngine, RoomService roomService,
                              LeaderboardService leaderboardService, AuthService authService) {
        this.gameEngine = gameEngine;
        this.roomService = roomService;
        this.leaderboardService = leaderboardService;
        this.authService = authService;
    }

    @PostMapping("/retry-distances")
    public ResponseEntity<?> retryDistances(@RequestParam String gameMode, HttpServletRequest request, HttpServletResponse response) {
        String token = PlayerCookies.getOrCreate(request, response);
        String userId = authService.resolveUser(request, response).map(UserAccount::getId).orElse(null);
        LoadedSession existing = requireGame(token, userId);

        GameMode mode = GameMode.valueOf(gameMode);
        boolean success = gameEngine.retryDistances(mode);

        if (success) {
            // recreate the game with the now-cached real distances.
            // mark the old session done so the player doesn't end up with two actives.
            roomService.markCompleted(existing);
            roomService.createSoloGame(token, userId, existing.gameState().getTeamName(), mode);
            return ResponseEntity.ok(Map.of("success", true));
        }

        return ResponseEntity.status(503).body(Map.of("success", false, "error", "API still unavailable"));
    }

    @PostMapping("/action")
    public ResponseEntity<?> processActionApi(@RequestParam String action, HttpServletRequest request, HttpServletResponse response) {
        String token = PlayerCookies.getOrCreate(request, response);
        String userId = authService.resolveUser(request, response).map(UserAccount::getId).orElse(null);
        LoadedSession loaded = requireGame(token, userId);
        GameState gameState = loaded.gameState();

        TurnResult result;
        try {
            result = gameEngine.processAction(gameState, GameAction.valueOf(action));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError("Invalid action"));
        }
        roomService.persist(loaded);
        return ResponseEntity.ok(new ActionResponse(gameState, result));
    }

    @PostMapping("/choice")
    public ResponseEntity<ActionResponse> processChoice(@RequestParam int choiceIndex, HttpServletRequest request, HttpServletResponse response) {
        String token = PlayerCookies.getOrCreate(request, response);
        String userId = authService.resolveUser(request, response).map(UserAccount::getId).orElse(null);
        LoadedSession loaded = requireGame(token, userId);
        TurnResult result = gameEngine.resolveChoice(loaded.gameState(), choiceIndex);
        roomService.persist(loaded);
        return ResponseEntity.ok(new ActionResponse(loaded.gameState(), result));
    }

    @GetMapping("/market")
    public Map<String, Object> getMarket(HttpServletRequest request, HttpServletResponse response) {
        String token = PlayerCookies.getOrCreate(request, response);
        String userId = authService.resolveUser(request, response).map(UserAccount::getId).orElse(null);
        LoadedSession loaded = requireGame(token, userId);
        GameEvent market = gameEngine.getMarket(loaded.gameState());
        roomService.persist(loaded);
        return Map.of("event", market, "purchased", loaded.gameState().getMarketState().getMarketPurchased());
    }

    @PostMapping("/market")
    public ResponseEntity<?> processMarketPurchase(@RequestParam int choiceIndex, HttpServletRequest request, HttpServletResponse response) {
        String token = PlayerCookies.getOrCreate(request, response);
        String userId = authService.resolveUser(request, response).map(UserAccount::getId).orElse(null);
        LoadedSession loaded = requireGame(token, userId);

        MarketResult marketResult = gameEngine.buyFromMarket(loaded.gameState(), choiceIndex);
        if (!marketResult.ok()) {
            return ResponseEntity.badRequest().body(new ApiError(marketResult.error()));
        }
        roomService.persist(loaded);
        return ResponseEntity.ok(loaded.gameState());
    }

    @PostMapping("/leaderboard")
    public ResponseEntity<?> submitScore(@RequestParam String playerName, HttpServletRequest request, HttpServletResponse response) {
        String token = PlayerCookies.getOrCreate(request, response);
        String userId = authService.resolveUser(request, response).map(UserAccount::getId).orElse(null);
        LoadedSession loaded = requireGame(token, userId);

        boolean daily = roomService.isDailySession(loaded);
        SubmissionResult result = leaderboardService.submitResult(loaded.gameState(), playerName, daily);
        if (result.ok()) {
            roomService.markCompleted(loaded);
            return ResponseEntity.ok(Map.of("success", true));
        }
        roomService.persist(loaded);
        return ResponseEntity.badRequest().body(new ApiError(result.error()));
    }

    private LoadedSession requireGame(String playerToken) {
        return requireGame(playerToken, null);
    }

    private LoadedSession requireGame(String playerToken, String userId) {
        return roomService.loadActiveSession(playerToken, userId).orElseThrow(NoGameInSessionException::new);
    }
}
