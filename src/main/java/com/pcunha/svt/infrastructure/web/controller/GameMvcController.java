package com.pcunha.svt.infrastructure.web.controller;

import com.pcunha.svt.application.ConditionEvaluator;
import com.pcunha.svt.application.GameEngine;
import com.pcunha.svt.application.LeaderboardService;
import com.pcunha.svt.application.ScoreCalculator;
import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.infrastructure.data.GameDataLoader;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class GameMvcController {

    private final GameEngine gameEngine;
    private final LeaderboardService leaderboardService;

    public GameMvcController(GameEngine gameEngine, LeaderboardService leaderboardService) {
        this.gameEngine = gameEngine;
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("gameModes", GameMode.values());
        return "start";
    }

    @PostMapping("/start")
    public String createGame(@RequestParam String teamName, @RequestParam(defaultValue = "FAST") String gameMode, HttpSession session) {
        if (teamName == null || teamName.trim().isEmpty()) {
            return "redirect:/";
        }
        GameMode mode = GameMode.valueOf(gameMode);
        GameState gameState = gameEngine.createNewGame(teamName.trim(), mode);
        session.setAttribute("gameState", gameState);
        return "redirect:/game";
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
        if (gameState == null) return "redirect:/";

        try {
            gameEngine.processAction(gameState, GameAction.valueOf(action));
        } catch (IllegalArgumentException e) {
            return "redirect:/game";
        }

        if (gameState.getEndingState().isGameOver()) {
            return "redirect:/end";
        }
        return "redirect:/game";
    }

    @GetMapping("/end")
    public String endGame(HttpSession session, Model model) {
        GameState gameState = getGameState(session);
        if (gameState == null) return "redirect:/";
        model.addAttribute("gameState", gameState);
        model.addAttribute("score", ScoreCalculator.calculate(gameState));
        model.addAttribute("submitted", gameState.getEndingState().isLeaderboardSubmitted());
        return "end";
    }

    @GetMapping("/leaderboard")
    public String leaderboard(Model model) {
        model.addAttribute("leaderboard", leaderboardService.getTopScoresByMode());
        return "leaderboard";
    }

    private GameState getGameState(HttpSession session) {
        return (GameState) session.getAttribute("gameState");
    }
}
