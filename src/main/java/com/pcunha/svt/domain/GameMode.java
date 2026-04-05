package com.pcunha.svt.domain;

import lombok.Getter;

@Getter
public enum GameMode {
    FAST("Fast", "Straight-line distance", 1.0),
    ROAD("Road", "Real driving distance", 1.0),
    WALKING_ROAD("Walking (Road)", "Slow pace, real roads", 0.4),
    WALKING_FAST("Walking (Fast)", "Slow pace, straight-line", 0.4);

    private final String displayName;
    private final String description;
    private final double speedMultiplier;

    GameMode(String displayName, String description, double speedMultiplier) {
        this.displayName = displayName;
        this.description = description;
        this.speedMultiplier = speedMultiplier;
    }
}
