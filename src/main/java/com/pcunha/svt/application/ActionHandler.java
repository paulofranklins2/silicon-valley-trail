package com.pcunha.svt.application;

import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.GameState;

import java.util.Random;

public class ActionHandler {
    private final Random random;
    private static final double TRAVEL_DISTANCE = 5.0;

    public ActionHandler(Random random) {
        this.random = random;
    }

    private boolean randomChance() {
        return this.random.nextBoolean();
    }

    public void handle(GameState gameState, GameAction gameAction) {
        switch (gameAction) {
            case TRAVEL -> travel(gameState);
            case REST -> rest(gameState);
            case SCAVENGE -> scavenge(gameState);
            case HACKATHON -> hackathon(gameState);
            case PITCH_VCS -> pitchVcs(gameState);
        }
    }

    private void travel(GameState gameState) {
        // - distance,  - energy. - food, - computeCredit
        gameState.getJourneyState().travel(TRAVEL_DISTANCE);
        gameState.getTeamState().changeEnergy(-15);
        gameState.getResourceState().changeFood(-1);
        gameState.getResourceState().changeComputeCredits(-1);
    }

    private void rest(GameState gameState) {
        // + health, + energy, - food, + morale
        gameState.getTeamState().changeHealth(10);
        gameState.getTeamState().changeMorale(20);
        gameState.getTeamState().changeEnergy(5);
        gameState.getResourceState().changeFood(-1);

    }

    private void scavenge(GameState gameState) {
        // energy -10, random chance of food (+2) or cash (10)
        gameState.getTeamState().changeEnergy(-10);
        if (randomChance()) gameState.getResourceState().changeFood(2);
        else gameState.getResourceState().changeCash(10);
    }

    private void hackathon(GameState gameState) {
        // compute credit + 10, energy - 5, morale -5, food -1
        gameState.getTeamState().changeEnergy(-15);
        gameState.getTeamState().changeMorale(-5);
        gameState.getResourceState().changeComputeCredits(10);
        gameState.getResourceState().changeFood(-1);
    }

    private void pitchVcs(GameState gameState) {
        // random change off success (get cash 50) fail (loose morale - 10)
        if (randomChance()) gameState.getResourceState().changeCash(50);
        else gameState.getTeamState().changeMorale(-10);
    }
}
