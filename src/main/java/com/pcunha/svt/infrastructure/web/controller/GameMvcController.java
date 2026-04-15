package com.pcunha.svt.infrastructure.web.controller;

import com.pcunha.svt.application.GameEngine;
import com.pcunha.svt.application.LeaderboardService;
import com.pcunha.svt.application.RoomService;
import com.pcunha.svt.application.RoomService.LoadedSession;
import com.pcunha.svt.application.Tunables;
import com.pcunha.svt.application.AuthService;
import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.TurnResult;
import com.pcunha.svt.domain.model.UserAccount;
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

import java.util.ArrayList;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class GameMvcController {
    private static final DateTimeFormatter LEADERBOARD_DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private final GameEngine gameEngine;
    private final RoomService roomService;
    private final LeaderboardService leaderboardService;
    private final Tunables tunables;
    private final AuthService authService;

    public GameMvcController(GameEngine gameEngine, RoomService roomService,
                             LeaderboardService leaderboardService, Tunables tunables,
                             AuthService authService) {
        this.gameEngine = gameEngine;
        this.roomService = roomService;
        this.leaderboardService = leaderboardService;
        this.tunables = tunables;
        this.authService = authService;
    }

    @GetMapping("/")
    public String home(Model model, HttpServletRequest request, HttpServletResponse response) {
        String token = PlayerCookies.getOrCreate(request, response);
        UserAccount user = authService.resolveUser(request, response).orElse(null);
        Map<String, LoadedSession> activeSessions = roomService.loadActiveSessionsBySlot(token, user != null ? user.getId() : null);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();

        model.addAttribute("modeOptions", buildModeOptions(activeSessions, token, baseUrl, user != null));
        model.addAttribute("resumeLinks", buildResumeLinks(activeSessions, token, baseUrl, user != null));
        model.addAttribute("signedIn", user != null);
        model.addAttribute("authLogin", user != null ? user.getLogin() : null);
        return "start";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String login,
                         @RequestParam String password,
                         HttpServletRequest request, HttpServletResponse response,
                         RedirectAttributes redirectAttributes) {
        String token = PlayerCookies.getOrCreate(request, response);
        var result = authService.signup(login, password, token, request, response);
        if (!result.ok()) {
            redirectAttributes.addFlashAttribute("authError", result.error());
            return "redirect:/";
        }
        redirectAttributes.addFlashAttribute("authInfo", "Account created");
        return "redirect:/";
    }

    @PostMapping("/login")
    public String login(@RequestParam String login,
                        @RequestParam String password,
                        HttpServletRequest request, HttpServletResponse response,
                        RedirectAttributes redirectAttributes) {
        String token = PlayerCookies.getOrCreate(request, response);
        var result = authService.login(login, password, token, request, response);
        if (!result.ok()) {
            redirectAttributes.addFlashAttribute("authError", result.error());
            return "redirect:/";
        }
        redirectAttributes.addFlashAttribute("authInfo", "Logged in");
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return "redirect:/";
    }

    @PostMapping("/start")
    public String createGame(@RequestParam String teamName,
                             @RequestParam(defaultValue = "EASY") String gameMode,
                             HttpServletRequest request, HttpServletResponse response) {
        if (teamName == null || teamName.trim().isEmpty()) return "redirect:/";
        String token = PlayerCookies.getOrCreate(request, response);
        UserAccount user = authService.resolveUser(request, response).orElse(null);
        String userId = user != null ? user.getId() : null;

        if ("DAILY".equals(gameMode)) {
            roomService.loadActiveSessionForSlot(token, userId, "DAILY").ifPresent(roomService::markCompleted);
            roomService.createOrJoinDailyGame(token, userId, teamName.trim(), GameMode.EASY);
        } else {
            roomService.loadActiveSessionForSlot(token, userId, gameMode).ifPresent(roomService::markCompleted);
            roomService.createSoloGame(token, userId, teamName.trim(), GameMode.valueOf(gameMode));
        }
        return "redirect:/game";
    }

    @GetMapping("/game")
    public String getGame(HttpServletRequest request, HttpServletResponse response,
                          Model model, @ModelAttribute("turnResult") TurnResult turnResult) {
        String token = PlayerCookies.getOrCreate(request, response);
        UserAccount user = authService.resolveUser(request, response).orElse(null);
        LoadedSession loaded = roomService.loadActiveSession(token, user != null ? user.getId() : null).orElse(null);
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
        UserAccount user = authService.resolveUser(request, response).orElse(null);
        LoadedSession loaded = roomService.loadActiveSession(token, user != null ? user.getId() : null).orElse(null);
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

    @PostMapping("/quit")
    public String quitGame(HttpServletRequest request, HttpServletResponse response) {
        String token = PlayerCookies.getOrCreate(request, response);
        UserAccount user = authService.resolveUser(request, response).orElse(null);
        roomService.loadActiveSession(token, user != null ? user.getId() : null).ifPresent(roomService::markCompleted);
        return "redirect:/";
    }

    @GetMapping("/end")
    public String endGame(HttpServletRequest request, HttpServletResponse response, Model model) {
        String token = PlayerCookies.getOrCreate(request, response);
        UserAccount user = authService.resolveUser(request, response).orElse(null);
        LoadedSession loaded = roomService.loadActiveSession(token, user != null ? user.getId() : null).orElse(null);
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
        model.addAttribute("leaderboardTitle", "All-Time");
        return "leaderboard";
    }

    @GetMapping("/leaderboard/daily")
    public String dailyLeaderboard(Model model) {
        roomService.purgeExpiredDailyData();
        model.addAttribute("leaderboard", leaderboardService.getDailyTopScores());
        model.addAttribute("view", "daily");
        model.addAttribute("leaderboardTitle", "Today (" + LocalDate.now().format(LEADERBOARD_DATE_FORMAT) + ")");
        return "leaderboard";
    }

    @GetMapping("/resume-mode/{mode}")
    public String resumeMode(@PathVariable String mode,
                             HttpServletRequest request, HttpServletResponse response) {
        String token = PlayerCookies.getOrCreate(request, response);
        String userId = authService.resolveUser(request, response).map(UserAccount::getId).orElse(null);
        LoadedSession loaded = roomService.loadActiveSessionForSlot(token, userId, mode).orElse(null);
        if (loaded == null) return "redirect:/";
        roomService.touch(loaded);
        return loaded.gameState().getEndingState().isGameOver() ? "redirect:/end" : "redirect:/game";
    }

    @GetMapping("/resume/{token}")
    public String resume(@PathVariable String token, HttpServletRequest request, HttpServletResponse response) {
        // Validate the token is a real UUID before issuing the cookie. Stops
        // garbage strings (typos, scrapers) from polluting the cookie store.
        try {
            UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            return "redirect:/";
        }
        PlayerCookies.write(request, response, token);
        return "redirect:/game";
    }

    @GetMapping("/resume/{token}/{mode}")
    public String resumeModeOnAnotherDevice(@PathVariable String token, @PathVariable String mode,
                                            HttpServletRequest request, HttpServletResponse response) {
        try {
            UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            return "redirect:/";
        }
        PlayerCookies.write(request, response, token);
        return "redirect:/resume-mode/" + mode;
    }

    private List<StartModeOption> buildModeOptions(Map<String, LoadedSession> activeSessions, String token,
                                                   String baseUrl, boolean signedIn) {
        List<StartModeOption> options = new ArrayList<>();
        for (GameMode mode : GameMode.values()) {
            options.add(buildModeOption(mode.name(), mode.getDisplayName(), activeSessions.get(mode.name()), token, baseUrl, signedIn, true));
        }
        options.add(buildModeOption("DAILY", "Daily", activeSessions.get("DAILY"), token, baseUrl, signedIn, false));
        return options;
    }

    private List<ResumeLink> buildResumeLinks(Map<String, LoadedSession> activeSessions, String token,
                                              String baseUrl, boolean signedIn) {
        if (signedIn) return List.of();
        List<ResumeLink> links = new ArrayList<>();
        for (StartModeOption option : buildModeOptions(activeSessions, token, baseUrl, false)) {
            if (option.deviceResumeUrl() != null) {
                links.add(new ResumeLink(option.displayName(), option.deviceResumeUrl()));
            }
        }
        return links;
    }

    private StartModeOption buildModeOption(String code, String displayName, LoadedSession loaded,
                                            String token, String baseUrl, boolean signedIn, boolean allowNewWhenActive) {
        boolean active = loaded != null;
        boolean resultReady = active && loaded.gameState().getEndingState().isGameOver();
        String status = !active ? null : resultReady ? "Result ready" : "Run in progress";
        String deviceResumeUrl = active && !signedIn ? baseUrl + "/resume/" + token + "/" + code : null;
        return new StartModeOption(
                code,
                displayName,
                active,
                allowNewWhenActive || !active,
                resultReady ? "Result" : "Resume",
                active ? "New" : "Start",
                status,
                deviceResumeUrl
        );
    }

    public record StartModeOption(String code, String displayName, boolean resumeAvailable, boolean newAvailable,
                                  String resumeLabel, String newLabel, String statusLabel, String deviceResumeUrl) {
    }

    public record ResumeLink(String label, String url) {
    }
}
