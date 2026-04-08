package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.LossReason;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
public class EndingState implements Serializable {
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
