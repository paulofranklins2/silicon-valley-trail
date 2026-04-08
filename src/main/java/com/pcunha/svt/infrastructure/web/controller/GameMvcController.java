package com.pcunha.svt.infrastructure.web.controller;

import com.pcunha.svt.application.GameEngine;
import com.pcunha.svt.application.LeaderboardService;
import com.pcunha.svt.application.RoomService;
import com.pcunha.svt.application.RoomService.LoadedSession;
import com.pcunha.svt.application.Tunables;
import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.TurnResult;
import com.pcunha.svt.infrastructure.data.GameDataLoader;
import com.pcunha.svt.infrastructure.web.PlayerCookies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@Controller
public class GameMvcController {

    private final GameEngine gameEngine;
    private final RoomService roomService;
    private final LeaderboardService leaderboardService;
    private final Tunables tunables;

    public GameMvcController(GameEngine gameEngine, RoomService roomService,
                             LeaderboardService leaderboardService, Tunables tunables) {
        this.gameEngine = gameEngine;
        this.roomService = roomService;
        this.leaderboardService = leaderboardService;
        this.tunables = tunables;
    }

    @GetMapping("/")
    public String home(Model model, HttpServletRequest request, HttpServletResponse response) {
        String token = PlayerCookies.getOrCreate(request, response);
        LoadedSession loaded = roomService.loadActiveSession(token).orElse(null);
        boolean canResume = loaded != null && !loaded.gameState().getEndingState().isGameOver();
        boolean hasUnfinishedEnd = loaded != null && loaded.gameState().getEndingState().isGameOver();

        model.addAttribute("gameModes", GameMode.values());
        model.addAttribute("canResume", canResume);
        model.addAttribute("hasUnfinishedEnd", hasUnfinishedEnd);
        if (canResume || hasUnfinishedEnd) {
            String resumeUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/resume/")
                    .path(token)
                    .toUriString();
            model.addAttribute("resumeUrl", resumeUrl);
        }
        return "start";
    }

    @PostMapping("/start")
    public String createGame(@RequestParam String teamName,
                             @RequestParam(defaultValue = "EASY") String gameMode,
                             HttpServletRequest request, HttpServletResponse response) {
        if (teamName == null || teamName.trim().isEmpty()) return "redirect:/";
        String token = PlayerCookies.getOrCreate(request, response);

        // Abandon any prior active game so this player has exactly one in flight.
        roomService.loadActiveSession(token).ifPresent(roomService::markCompleted);

        if ("DAILY".equals(gameMode)) {
            roomService.createOrJoinDailyGame(token, teamName.trim(), GameMode.EASY);
        } else {
            roomService.createSoloGame(token, teamName.trim(), GameMode.valueOf(gameMode));
        }
        return "redirect:/game";
    }

    @GetMapping("/game")
    public String getGame(HttpServletRequest request, HttpServletResponse response,
                          Model model, @ModelAttribute("turnResult") TurnResult turnResult) {
        String token = PlayerCookies.getOrCreate(request, response);
        LoadedSession loaded = roomService.loadActiveSession(token).orElse(null);
        if (loaded == null) return "redirect:/";
        GameState gameState = loaded.gameState();
        if (gameState.getEndingState().isGameOver()) return "redirect:/end";

        model.addAttribute("gameState", gameState);
        model.addAttribute("actions", GameDataLoader.loadActions());
        model.addAttribute("foodGraceTurns", tunables.foodGraceTurns());
        model.addAttribute("cashGraceTurns", tunables.cashGraceTurns());
        model.addAttribute("turnResult", turnResult);
        return "game";
    }

    @PostMapping("/action")
    public String processAction(@RequestParam String action,
                                HttpServletRequest request, HttpServletResponse response,
                                RedirectAttributes redirectAttributes) {
        String token = PlayerCookies.getOrCreate(request, response);
        LoadedSession loaded = roomService.loadActiveSession(token).orElse(null);
        if (loaded == null) return "redirect:/";

        TurnResult result;
        try {
            result = gameEngine.processAction(loaded.gameState(), GameAction.valueOf(action));
        } catch (IllegalArgumentException e) {
            return "redirect:/game";
        }

        try {
            roomService.persist(loaded);
        } catch (OptimisticLockingFailureException e) {
            // Two-tab race: another request beat us to the save. Send the
            // player back to /game so they reload the latest state.
            return "redirect:/game";
        }

        if (loaded.gameState().getEndingState().isGameOver()) return "redirect:/end";
        redirectAttributes.addFlashAttribute("turnResult", result);
        return "redirect:/game";
    }

    @GetMapping("/end")
    public String endGame(HttpServletRequest request, HttpServletResponse response, Model model) {
        String token = PlayerCookies.getOrCreate(request, response);
        LoadedSession loaded = roomService.loadActiveSession(token).orElse(null);
        if (loaded == null) return "redirect:/";
        GameState gameState = loaded.gameState();
        model.addAttribute("gameState", gameState);
        model.addAttribute("score", leaderboardService.calculateScore(gameState));
        model.addAttribute("submitted", gameState.getEndingState().isLeaderboardSubmitted());
        return "end";
    }

    @GetMapping("/leaderboard")
    public String leaderboard(Model model) {
        model.addAttribute("leaderboard", leaderboardService.getTopScores());
        model.addAttribute("view", "overall");
        return "leaderboard";
    }

    @GetMapping("/leaderboard/daily")
    public String dailyLeaderboard(Model model) {
        model.addAttribute("leaderboard", leaderboardService.getDailyTopScores());
        model.addAttribute("view", "daily");
        return "leaderboard";
    }

    @GetMapping("/resume/{token}")
    public String resume(@PathVariable String token, HttpServletResponse response) {
        // Validate the token is a real UUID before issuing the cookie. Stops
        // garbage strings (typos, scrapers) from polluting the cookie store.
        try {
            UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            return "redirect:/";
        }
        PlayerCookies.write(response, token);
        return "redirect:/game";
    }
}
