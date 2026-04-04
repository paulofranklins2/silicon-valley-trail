package com.pcunha.svt.infrastructure.web.controller;

import com.pcunha.svt.application.GameEngine;
import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.model.GameEvent;
import com.pcunha.svt.domain.model.GameState;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Controller
public class GameController {
    private final GameEngine gameEngine;

    public GameController(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    @GetMapping("/")
    public String home() {
        return "start";
    }

    @PostMapping("/start")
    public String createGame(@RequestParam String teamName, HttpSession session) {
        // clear stale market data from previous game
        session.removeAttribute("currentMarketEvent");
        session.removeAttribute("marketCityIndex");
        session.removeAttribute("marketPurchased");

        GameState gameState = gameEngine.createNewGame(teamName);
        session.setAttribute("gameState", gameState);
        return "redirect:/game";
    }

    @GetMapping("/game")
    public String getGame(HttpSession session, Model model) {
        GameState gameState = (GameState) session.getAttribute("gameState");
        if (gameState == null) {
            return "redirect:/";
        }
        model.addAttribute("gameState", gameState);
        model.addAttribute("actions", GameAction.values());
        return "game";
    }

    @PostMapping("/action")
    public String processAction(@RequestParam String action, HttpSession session) {
        GameState gameState = (GameState) session.getAttribute("gameState");
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
        GameState gameState = (GameState) session.getAttribute("gameState");
        if (gameState == null) {
            return "redirect:/";
        }
        gameEngine.processAction(gameState, GameAction.valueOf(action));
        return gameState;
    }

    @PostMapping("/api/choice")
    @ResponseBody
    public Object processChoice(@RequestParam int choiceIndex, HttpSession session) {
        GameState gameState = (GameState) session.getAttribute("gameState");
        if (gameState == null) {
            return "redirect:/";
        }
        gameEngine.resolveChoice(gameState, choiceIndex);
        return gameState;
    }

    @GetMapping("/api/market")
    @ResponseBody
    public Object getMarket(HttpSession session) {
        GameState gameState = (GameState) session.getAttribute("gameState");
        if (gameState == null) return "redirect:/";

        int cityIndex = gameState.getJourneyState().getCurrentLocationIndex();
        Integer storedCityIndex = (Integer) session.getAttribute("marketCityIndex");
        GameEvent marketEvent = (GameEvent) session.getAttribute("currentMarketEvent");

        // same city → return the same market; new city → generate fresh market
        if (marketEvent == null || storedCityIndex == null || storedCityIndex != cityIndex) {
            marketEvent = gameEngine.getCityMarketEvent();
            session.setAttribute("currentMarketEvent", marketEvent);
            session.setAttribute("marketCityIndex", cityIndex);
            session.setAttribute("marketPurchased", new HashSet<Integer>());
        }

        @SuppressWarnings("unchecked")
        Set<Integer> purchased = (Set<Integer>) session.getAttribute("marketPurchased");
        return Map.of("event", marketEvent, "purchased", purchased);
    }

    @PostMapping("/api/market")
    @ResponseBody
    public Object processMarketPurchase(@RequestParam int choiceIndex, HttpSession session) {
        GameState gameState = (GameState) session.getAttribute("gameState");
        if (gameState == null) return "redirect:/";

        GameEvent marketEvent = (GameEvent) session.getAttribute("currentMarketEvent");
        @SuppressWarnings("unchecked")
        Set<Integer> purchased = (Set<Integer>) session.getAttribute("marketPurchased");
        if (purchased == null) purchased = new HashSet<>();

        // block duplicate purchases
        boolean isSkip = marketEvent != null && marketEvent.getOutcomes() != null
                && choiceIndex == marketEvent.getOutcomes().size() - 1;
        if (!isSkip && purchased.contains(choiceIndex)) {
            return Map.of("state", gameState, "purchased", purchased, "error", "Already purchased");
        }

        boolean success = gameEngine.resolveMarketPurchase(gameState, marketEvent, choiceIndex);
        if (!success) {
            return Map.of("state", gameState, "purchased", purchased, "error", "Not enough cash");
        }
        if (!isSkip) {
            purchased.add(choiceIndex);
            session.setAttribute("marketPurchased", purchased);
        }

        return Map.of("state", gameState, "purchased", purchased);
    }

    @GetMapping("/end")
    public String endGame(HttpSession session, Model model) {
        GameState gameState = (GameState) session.getAttribute("gameState");
        if (gameState == null) return "redirect:/";
        model.addAttribute("gameState", gameState);
        return "end";
    }

}
