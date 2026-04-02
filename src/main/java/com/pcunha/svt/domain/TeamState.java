package com.pcunha.svt.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class TeamState {
    private int health;
    private int energy;
    private int morale;

    public void changeHealth(int delta) {
        this.health = Math.clamp(this.health + delta, 0, 100);
    }

    public void changeEnergy(int delta) {
        this.energy = Math.clamp(this.energy + delta, 0, 100);
    }

    public void changeMorale(int delta) {
        this.morale = Math.clamp(this.morale + delta, 0, 100);
    }

}
