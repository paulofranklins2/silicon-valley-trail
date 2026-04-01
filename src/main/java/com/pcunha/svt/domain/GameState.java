package com.pcunha.svt.domain;

import lombok.Getter;

@Getter
public class GameState {
    private TeamState teamState;
    private ResourceState resourceState;
    private JourneyState journeyState;
    private int turn;
    private boolean victory;
    private boolean gameOver;

    public GameState(TeamState teamState, ResourceState resourceState, JourneyState journeyState) {
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

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }
}
