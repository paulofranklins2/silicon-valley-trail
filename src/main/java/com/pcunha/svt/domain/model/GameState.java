package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.ActionOutcome;
import com.pcunha.svt.domain.GameAction;
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
    @Setter
    private GameAction lastAction;
    private int turn;
    private boolean victory;
    @Setter
    private boolean gameOver;
    @Setter
    private ActionOutcome lastActionResult;

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
