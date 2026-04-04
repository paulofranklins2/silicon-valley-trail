package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Getter
@ToString
public class GameState {
    private String teamName;
    private TeamState teamState;
    private ResourceState resourceState;
    private JourneyState journeyState;
    @Setter
    private GameEvent lastEvent;
    @Setter
    private GameAction lastAction;
    @Setter
    private WeatherCategory lastWeather;
    @Setter
    private double lastWeatherTemp;
    private int turn;
    private boolean victory;
    @Setter
    private boolean gameOver;
    @Setter
    private ActionOutcome lastActionResult;
    @Setter
    private boolean waitingEventChoice;
    private int turnWithoutCash;
    private int turnWithoutFood;
    @Setter
    private LossReason lossReason;
    @Setter
    private GameEvent currentMarketEvent;
    @Setter
    private int marketCityIndex = -1;
    private Set<Integer> marketPurchased = new HashSet<>();
    @Setter
    private GameMode gameMode;
    @Setter
    private boolean leaderboardSubmitted;
    @Setter
    private boolean usedFallbackDistances;

    public GameState(TeamState teamState, ResourceState resourceState, JourneyState journeyState, String teamName) {
        this.teamName = teamName;
        this.teamState = teamState;
        this.resourceState = resourceState;
        this.journeyState = journeyState;
        this.turn = 1;
        this.victory = false;
    }

    public void nextTurn() {
        turn++;
    }

    public void setVictory(boolean victory) {
        this.victory = victory;
        this.gameOver = true;
    }

    public void incrementTurnWithoutFood() {
        this.turnWithoutFood++;
    }

    public void incrementTurnWithoutCash() {
        this.turnWithoutCash++;
    }

    public void resetTurnWithoutFood() {
        this.turnWithoutFood = 0;
    }

    public void resetTurnWithoutCash() {
        this.turnWithoutCash = 0;
    }

    public void resetMarket() {
        this.currentMarketEvent = null;
        this.marketCityIndex = -1;
        this.marketPurchased = new HashSet<>();
    }

    public void addMarketPurchase(int index) {
        this.marketPurchased.add(index);
    }
}
