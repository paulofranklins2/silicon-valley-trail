package com.pcunha.svt.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
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
