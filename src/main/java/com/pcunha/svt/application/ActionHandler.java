package com.pcunha.svt.application;

import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.GameState;

import java.util.Random;

public class ActionHandler {
    private final Random random;
    private static final double TRAVEL_DISTANCE = 5.0;
    // consts travel
    private static final int TRAVEL_ENERGY = -15;
    private static final int TRAVEL_FOOD = -1;
    private static final int TRAVEL_COMPUTE_CREDIT = -1;
    // consts rest
    private static final int REST_HEALTH = 10;
    private static final int REST_ENERGY = 5;
    private static final int REST_MORALE = 20;
    private static final int REST_FOOD = -1;
    // consts scavenge
    private static final int SCAVENGE_ENERGY = -10;
    private static final int SCAVENGE_FOOD = 2;
    private static final int SCAVENGE_CASH = 10;
    // consts hackathon
    private static final int HACKATHON_ENERGY = -15;
    private static final int HACKATHON_MORALE = -5;
    private static final int HACKATHON_COMPUTE_CREDIT = 10;
    private static final int HACKATHON_FOOD = -1;
    // consts pitch vcs
    private static final int PITCH_VCS_MORALE = -10;
    private static final int PITCH_VCS_CASH = 50;

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
        // -distance,  -energy, -food, -computeCredit
        gameState.getJourneyState().travel(TRAVEL_DISTANCE);
        gameState.getTeamState().changeEnergy(TRAVEL_ENERGY);
        gameState.getResourceState().changeFood(TRAVEL_FOOD);
        gameState.getResourceState().changeComputeCredits(TRAVEL_COMPUTE_CREDIT);
    }

    private void rest(GameState gameState) {
        // +health, +energy, -food, +morale
        gameState.getTeamState().changeHealth(REST_HEALTH);
        gameState.getTeamState().changeEnergy(REST_ENERGY);
        gameState.getResourceState().changeFood(REST_FOOD);
        gameState.getTeamState().changeMorale(REST_MORALE);
    }

    private void scavenge(GameState gameState) {
        // -energy, random chance of +food or +cash
        gameState.getTeamState().changeEnergy(SCAVENGE_ENERGY);
        if (randomChance()) {
            gameState.getResourceState().changeFood(SCAVENGE_FOOD);
        } else {
            gameState.getResourceState().changeCash(SCAVENGE_CASH);
        }
    }

    private void hackathon(GameState gameState) {
        // +computeCredit, -energy, -morale, -food
        gameState.getTeamState().changeEnergy(HACKATHON_ENERGY);
        gameState.getTeamState().changeMorale(HACKATHON_MORALE);
        gameState.getResourceState().changeComputeCredits(HACKATHON_COMPUTE_CREDIT);
        gameState.getResourceState().changeFood(HACKATHON_FOOD);
    }

    private void pitchVcs(GameState gameState) {
        // random change of success +cash, fail -morale
        if (randomChance()) {
            gameState.getResourceState().changeCash(PITCH_VCS_CASH);
        } else {
            gameState.getTeamState().changeMorale(PITCH_VCS_MORALE);
        }
    }
}
