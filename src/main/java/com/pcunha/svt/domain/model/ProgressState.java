package com.pcunha.svt.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public class ProgressState {
    private int turn = 1;
    @Setter
    private TurnResult lastTurnResult = new TurnResult();

    public void nextTurn() {
        turn++;
    }
}
