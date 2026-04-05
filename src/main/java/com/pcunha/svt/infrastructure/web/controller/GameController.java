package com.pcunha.svt.infrastructure.web.controller;

import com.pcunha.svt.application.ConditionEvaluator;
import com.pcunha.svt.application.GameEngine;
import com.pcunha.svt.application.ScoreCalculator;
import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.model.GameEvent;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.LeaderboardEntry;
import com.pcunha.svt.domain.model.MarketResult;
import com.pcunha.svt.domain.port.LeaderboardPort;
import com.pcunha.svt.infrastructure.data.GameDataLoader;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class GameController {
    private final GameEngine gameEngine;
    private final LeaderboardPort leaderboardPort;

    public GameController(GameEngine gameEngine, LeaderboardPort leaderboardPort) {
        this.gameEngine = gameEngine;
        this.leaderboardPort = leaderboardPort;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("walkingAvailable", gameEngine.isModeAvailable(GameMode.WALKING));
        return "start";
    }

    @PostMapping("/start")
    public String createGame(@RequestParam String teamName, @RequestParam(defaultValue = "FAST") String gameMode, HttpSession session) {
        GameMode mode = GameMode.valueOf(gameMode);
        GameState gameState = gameEngine.createNewGame(teamName, mode);
        session.setAttribute("gameState", gameState);
        return "redirect:/game";
    }

    @PostMapping("/api/retry-distances")
    @ResponseBody
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

    @GetMapping("/game")
    public String getGame(HttpSession session, Model model) {
        GameState gameState = getGameState(session);
        if (gameState == null) {
            return "redirect:/";
        }
        model.addAttribute("gameState", gameState);
        model.addAttribute("actions", GameDataLoader.loadActions());
        model.addAttribute("foodGraceTurns", ConditionEvaluator.FOOD_GRACE_TURNS);
        model.addAttribute("cashGraceTurns", ConditionEvaluator.CASH_GRACE_TURNS);
        return "game";
    }

    @PostMapping("/action")
    public String processAction(@RequestParam String action, HttpSession session) {
        GameState gameState = getGameState(session);
        if (gameState == null) {
            return "redirect:/";
        }
        gameEngine.processAction(gameState, GameAction.valueOf(action));
        if (gameState.isGameOver()) {
            return "redirect:/end";
        }
        return "redirect:/game";
    }

    @PostMapping("/api/action")
    @ResponseBody
    public Object processActionApi(@RequestParam String action, HttpSession session) {
        GameState gameState = getGameState(session);
        if (gameState == null) {
            return "redirect:/";
        }
        gameEngine.processAction(gameState, GameAction.valueOf(action));
        return gameState;
    }

    @PostMapping("/api/choice")
    @ResponseBody
    public Object processChoice(@RequestParam int choiceIndex, HttpSession session) {
        GameState gameState = getGameState(session);
        if (gameState == null) {
            return "redirect:/";
        }
        gameEngine.resolveChoice(gameState, choiceIndex);
        return gameState;
    }

    @GetMapping("/api/market")
    @ResponseBody
    public Object getMarket(HttpSession session) {
        GameState gameState = getGameState(session);
        if (gameState == null) return "redirect:/";

        GameEvent market = gameEngine.getMarket(gameState);
        return Map.of("event", market, "purchased", gameState.getMarketPurchased());
    }

    @PostMapping("/api/market")
    @ResponseBody
    public Object processMarketPurchase(@RequestParam int choiceIndex, HttpSession session) {
        GameState gameState = getGameState(session);
        if (gameState == null) return "redirect:/";

        MarketResult marketResult = gameEngine.buyFromMarket(gameState, choiceIndex);
        if (!marketResult.ok()) {
            return Map.of("state", gameState, "purchased", gameState.getMarketPurchased(), "error", marketResult.error());
        }

        return Map.of("state", gameState, "purchased", gameState.getMarketPurchased());
    }

    @GetMapping("/end")
    public String endGame(HttpSession session, Model model) {
        GameState gameState = getGameState(session);
        if (gameState == null) return "redirect:/";
        model.addAttribute("gameState", gameState);
        model.addAttribute("score", ScoreCalculator.calculate(gameState));
        model.addAttribute("submitted", gameState.isLeaderboardSubmitted());
        return "end";
    }

    @PostMapping("/api/leaderboard")
    @ResponseBody
    public Object submitScore(@RequestParam String playerName, HttpSession session) {
        GameState gameState = getGameState(session);
        if (gameState == null) {
            return Map.of("error", "No game in progress");
        }
        if (gameState.isLeaderboardSubmitted()) {
            return Map.of("error", "Already submitted");
        }

        LeaderboardEntry entry = LeaderboardEntry.fromGameState(gameState, playerName);
        leaderboardPort.save(entry);
        gameState.setLeaderboardSubmitted(true);
        return Map.of("success", true);
    }

    @GetMapping("/leaderboard")
    public String leaderboard(Model model) {
        model.addAttribute("fastEntries", leaderboardPort.getTopScores(GameMode.FAST));
        model.addAttribute("roadEntries", leaderboardPort.getTopScores(GameMode.ROAD));
        model.addAttribute("walkingEntries", leaderboardPort.getTopScores(GameMode.WALKING));
        return "leaderboard";
    }

    private GameState getGameState(HttpSession session) {
        return (GameState) session.getAttribute("gameState");
    }
}
