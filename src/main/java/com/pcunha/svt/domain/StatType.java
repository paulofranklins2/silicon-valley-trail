package com.pcunha.svt.domain;

import lombok.Getter;

@Getter
public enum StatType {
    HEALTH("HEALTH"),
    ENERGY("ENERGY"),
    MORALE("MORALE"),
    CASH("CASH"),
    FOOD("FOOD"),
    COMPUTE_CREDIT("COMPUTE CREDIT");

    private final String displayName;

    StatType(String displayName) {
        this.displayName = displayName;
    }
}
