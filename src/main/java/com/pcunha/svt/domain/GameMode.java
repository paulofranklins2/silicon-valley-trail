package com.pcunha.svt.domain;

import lombok.Getter;

@Getter
public enum GameMode {
    FAST("Fast", "Straight-line distance - shorter journey"),
    ROAD("Road", "Real driving distance - longer journey");

    private final String displayName;
    private final String description;

    GameMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
