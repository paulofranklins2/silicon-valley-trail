package com.pcunha.svt.infrastructure.web.controller;

import com.pcunha.svt.application.GameEngine;
import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.GameState;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class GameController {
    private final GameEngine gameEngine;

    public GameController(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    @GetMapping("/")
    public String home() {
        // renders templates/start.html
        return "start";
    }

    @PostMapping("/start")
    public String createGame(@RequestParam String teamName, HttpSession session) {
        GameState gameState = gameEngine.createNewGame(teamName);
        session.setAttribute("gameState", gameState);
        return "redirect:/game";
    }

    @GetMapping("/game")
    public String getGame(HttpSession session, Model model) {
        GameState gameState = (GameState) session.getAttribute("gameState");
        model.addAttribute("gameState", gameState);
        model.addAttribute("actions", GameAction.values());
        return "game";
    }

    @PostMapping("/action")
    public String processAction(@RequestParam String action, HttpSession session) {
        GameState gameState = (GameState) session.getAttribute("gameState");
        gameEngine.processAction(gameState, GameAction.valueOf(action));
        if (gameState.isGameOver()) {
            return "redirect:/end";
        }
        return "redirect:/game";
    }

    @PostMapping("/api/action")
    @ResponseBody
    public GameState processActionApi(@RequestParam String action, HttpSession session) {
        GameState gameState = (GameState) session.getAttribute("gameState");
        gameEngine.processAction(gameState, GameAction.valueOf(action));
        return gameState;
    }

    @GetMapping("/end")
    public String endGame(HttpSession session, Model model) {
        GameState gameState = (GameState) session.getAttribute("gameState");
        model.addAttribute("gameState", gameState);
        // renders templates/end.html
        return "end";
    }

}
