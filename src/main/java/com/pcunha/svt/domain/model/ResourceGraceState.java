package com.pcunha.svt.domain.model;

import lombok.Getter;

@Getter
public class ResourceGraceState {
    private int turnWithoutCash;
    private int turnWithoutFood;

    public void incrementTurnWithoutFood() {
        this.turnWithoutFood++;
    }

    public void incrementTurnWithoutCash() {
        this.turnWithoutCash++;
    }

    public void resetTurnWithoutFood() {
        this.turnWithoutFood = 0;
    }

    public void resetTurnWithoutCash() {
        this.turnWithoutCash = 0;
    }
}
