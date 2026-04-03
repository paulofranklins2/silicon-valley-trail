package com.pcunha.svt.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class ResourceState {
    private int cash;
    private int food;
    private int computeCredits;

    public void changeCash(int delta) {
        this.cash = Math.max(0, this.cash + delta);
    }

    public void changeFood(int delta) {
        this.food = Math.max(0, this.food + delta);
    }

    public void changeComputeCredits(int delta) {
        this.computeCredits = Math.max(0, this.computeCredits + delta);
    }
}
