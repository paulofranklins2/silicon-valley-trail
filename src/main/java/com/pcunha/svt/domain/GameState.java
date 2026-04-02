package com.pcunha.svt.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
public class GameState {
    private String teamName;
    private TeamState teamState;
    private ResourceState resourceState;
    private JourneyState journeyState;
    @Setter
    private GameEvent lastEvent;
    private int turn;
    private boolean victory;
    @Setter
    private boolean gameOver;

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

}
