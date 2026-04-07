package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.LossReason;
import lombok.Getter;
import lombok.Setter;

@Getter
public class EndingState {
    private boolean victory;
    @Setter
    private boolean gameOver;
    @Setter
    private LossReason lossReason;
    private boolean isLeaderboardSubmitted;

    public void markVictory() {
        this.victory = true;
        this.gameOver = true;
    }

    public void markLeaderboardSubmitted() {
        this.isLeaderboardSubmitted = true;
    }
}
