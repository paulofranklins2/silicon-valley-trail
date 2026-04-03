package com.pcunha.svt.domain;

import com.pcunha.svt.domain.model.TeamState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamStateTest {

    @Test
    public void addHealth() {
        TeamState teamState = new TeamState(100, 100, 100);
        teamState.changeHealth(10);
        assertEquals(100, teamState.getHealth());
    }

    @Test
    public void subHealth() {
        TeamState teamState = new TeamState(100, 100, 100);
        teamState.changeHealth(-10);
        assertEquals(90, teamState.getHealth());
    }

    @Test
    public void addEnergy() {
        TeamState teamState = new TeamState(100, 100, 100);
        teamState.changeEnergy(10);
        assertEquals(100, teamState.getEnergy());
    }

    @Test
    public void subEnergy() {
        TeamState teamState = new TeamState(100, 100, 100);
        teamState.changeEnergy(-10);
        assertEquals(90, teamState.getEnergy());
    }

    @Test
    public void addMorale() {
        TeamState teamState = new TeamState(100, 100, 100);
        teamState.changeMorale(10);
        assertEquals(100, teamState.getMorale());
    }

    @Test
    public void subMorale() {
        TeamState teamState = new TeamState(100, 100, 100);
        teamState.changeMorale(-10);
        assertEquals(90, teamState.getMorale());
    }

    @Test
    public void healthClampAtZero() {
        TeamState teamState = new TeamState(10, 10, 100);
        teamState.changeHealth(-50);
        assertEquals(0, teamState.getHealth());
    }

    @Test
    public void healthClampAtMax() {
        TeamState teamState = new TeamState(90, 100, 100);
        teamState.changeHealth(30);
        assertEquals(100, teamState.getHealth());
    }

    @Test
    public void energyClampAtMax() {
        TeamState teamState = new TeamState(100, 90, 100);
        teamState.changeEnergy(30);
        assertEquals(100, teamState.getEnergy());
    }

    @Test
    public void energyClampAtZero() {
        TeamState teamState = new TeamState(100, 10, 100);
        teamState.changeEnergy(-50);
        assertEquals(0, teamState.getEnergy());
    }

    @Test
    public void moraleClampAtMax() {
        TeamState teamState = new TeamState(100, 100, 90);
        teamState.changeMorale(30);
        assertEquals(100, teamState.getMorale());
    }

    @Test
    public void moralClampAtZero() {
        TeamState teamState = new TeamState(100, 100, 10);
        teamState.changeMorale(-30);
        assertEquals(0, teamState.getMorale());
    }
}