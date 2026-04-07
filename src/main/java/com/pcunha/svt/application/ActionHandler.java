package com.pcunha.svt.application;

import com.pcunha.svt.domain.ActionOutcome;
import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.StatType;
import com.pcunha.svt.domain.model.ActionInfo;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.infrastructure.data.GameDataLoader;

import java.util.Map;
import java.util.Random;


public class ActionHandler {
    private final Random random;
    private final Map<GameAction, ActionInfo> actionMap;

    public ActionHandler(Random random, Map<GameAction, ActionInfo> actionMap) {
        this.random = random;
        this.actionMap = actionMap;
    }

    public static ActionHandler create(Random random) {
        return new ActionHandler(random, GameDataLoader.loadActionMap());
    }

    public ActionOutcome handle(GameState gameState, GameAction gameAction) {
        ActionInfo info = actionMap.get(gameAction);
        int energyCost = info.getEnergyCost();

        if (energyCost > 0 && gameState.getTeamState().getEnergy() < energyCost) {
            return ActionOutcome.EXHAUSTED;
        }

        return switch (gameAction) {
            case TRAVEL -> {
                travel(gameState, info);
                yield ActionOutcome.SUCCESS;
            }
            case REST, HACKATHON -> {
                applyAllEffects(gameState, info);
                yield ActionOutcome.SUCCESS;
            }
            case SCAVENGE -> scavenge(gameState, info);
            case PITCH_VCS -> pitchVcs(gameState, info);
        };
    }

    private void travel(GameState gameState, ActionInfo info) {
        double distance = info.getTravelDistance();
        distance *= gameState.getGameMode().getSpeedMultiplier();
        if (gameState.getResourceState().getComputeCredits() <= 0 && info.getComputePenaltyFactor() > 0) {
            distance *= info.getComputePenaltyFactor();
        }
        gameState.getJourneyState().travel(distance);
        applyAllEffects(gameState, info);
    }

    private ActionOutcome scavenge(GameState gameState, ActionInfo info) {
        gameState.getTeamState().changeEnergy(info.getEffect(StatType.ENERGY));
        if (random.nextBoolean()) {
            gameState.getResourceState().changeFood(info.getRandomEffect(StatType.FOOD));
            return ActionOutcome.FOOD;
        }
        gameState.getResourceState().changeCash(info.getRandomEffect(StatType.CASH));
        return ActionOutcome.CASH;
    }

    private ActionOutcome pitchVcs(GameState gameState, ActionInfo info) {
        // guaranteed costs
        gameState.getTeamState().changeEnergy(info.getEffect(StatType.ENERGY));
        gameState.getResourceState().changeComputeCredits(info.getEffect(StatType.COMPUTE_CREDIT));

        // random outcome
        if (random.nextBoolean()) {
            gameState.getResourceState().changeCash(info.getRandomEffect(StatType.CASH));
            return ActionOutcome.PITCH_SUCCESS;
        }
        gameState.getTeamState().changeMorale(info.getRandomEffect(StatType.MORALE));
        return ActionOutcome.PITCH_FAILURE;
    }

    private void applyAllEffects(GameState gameState, ActionInfo info) {
        for (ActionInfo.Effect fx : info.getEffects()) {
            if (fx.isRandom()) continue;
            applyStat(gameState, fx.getStat(), fx.getValue());
        }
    }

    private void applyStat(GameState gameState, StatType statType, int value) {
        switch (statType) {
            case HEALTH -> gameState.getTeamState().changeHealth(value);
            case ENERGY -> gameState.getTeamState().changeEnergy(value);
            case MORALE -> gameState.getTeamState().changeMorale(value);
            case CASH -> gameState.getResourceState().changeCash(value);
            case FOOD -> gameState.getResourceState().changeFood(value);
            case COMPUTE_CREDIT -> gameState.getResourceState().changeComputeCredits(value);
        }
    }
}
