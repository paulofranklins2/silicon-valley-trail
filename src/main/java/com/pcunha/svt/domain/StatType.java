package com.pcunha.svt.domain;

import lombok.Getter;

@Getter
public enum StatType {
    HEALTH("Health"),
    ENERGY("Energy"),
    MORALE("Morale"),
    CASH("Cash"),
    FOOD("Food"),
    COMPUTE_CREDIT("Cpu");

    private final String displayName;

    StatType(String displayName) {
        this.displayName = displayName;
    }
}
