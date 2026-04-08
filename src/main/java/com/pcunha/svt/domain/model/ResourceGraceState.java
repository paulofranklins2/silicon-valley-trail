package com.pcunha.svt.domain.model;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class ResourceGraceState implements Serializable {
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
