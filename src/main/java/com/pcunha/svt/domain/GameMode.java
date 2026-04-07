package com.pcunha.svt.domain;

import lombok.Getter;

@Getter
public enum GameMode {
    EASY("Easy", 1.0, 1.0),
    MEDIUM("Medium", 1.0, 1.3),
    HARD("Hard", 0.4, 1.7),
    IMPOSSIBLE("Impossible", 0.4, 2.2);

    private final String displayName;
    private final double speedMultiplier;
    private final double scoreMultiplier;

    GameMode(String displayName, double speedMultiplier, double scoreMultiplier) {
        this.displayName = displayName;
        this.speedMultiplier = speedMultiplier;
        this.scoreMultiplier = scoreMultiplier;
    }
}
