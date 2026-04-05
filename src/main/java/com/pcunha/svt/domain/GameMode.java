package com.pcunha.svt.domain;

import lombok.Getter;

@Getter
public enum GameMode {
    FAST("Fast", "Straight-line distance"),
    ROAD("Road", "Real driving distance"),
    WALKING("Walking", "Walking distance");

    private final String displayName;
    private final String description;

    GameMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
