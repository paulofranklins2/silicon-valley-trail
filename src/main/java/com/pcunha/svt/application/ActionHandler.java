package com.pcunha.svt.application;

import com.pcunha.svt.domain.ActionOutcome;
import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.StatType;
import com.pcunha.svt.domain.model.ActionEffect;
import com.pcunha.svt.domain.model.ActionInfo;
import com.pcunha.svt.domain.model.ActionResult;
import com.pcunha.svt.domain.model.ActionRoll;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.infrastructure.data.GameDataLoader;

import java.util.List;
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

    public ActionResult handle(GameState gameState, GameAction gameAction) {
        ActionInfo info = actionMap.get(gameAction);
        int energyCost = info.getEnergyCost();

        if (energyCost > 0 && gameState.getTeamState().getEnergy() < energyCost) {
            return new ActionResult(ActionOutcome.EXHAUSTED, null);
        }

        if (gameAction == GameAction.TRAVEL) {
            applyTravelDistance(gameState, info);
        }

        applyEffects(gameState, info.getEffects());
        ActionRoll chosen = pickRoll(info);
        if (chosen != null) {
            applyEffects(gameState, chosen.getEffects());
        }

        ActionOutcome outcome = classifyOutcome(chosen);
        return new ActionResult(outcome, chosen);
    }

    private void applyTravelDistance(GameState gameState, ActionInfo info) {
        double distance = info.getTravelDistance();
        distance *= gameState.getConfigState().getGameMode().getSpeedMultiplier();
        if (gameState.getResourceState().getComputeCredits() <= 0 && info.getComputePenaltyFactor() > 0) {
            distance *= info.getComputePenaltyFactor();
        }
        gameState.getJourneyState().travel(distance);
    }

    private ActionRoll pickRoll(ActionInfo info) {
        if (!info.hasOutcomes()) {
            return null;
        }
        List<ActionRoll> outcomes = info.getOutcomes();
        return outcomes.get(random.nextInt(outcomes.size()));
    }

    private void applyEffects(GameState gameState, List<ActionEffect> effects) {
        if (effects == null) {
            return;
        }
        for (ActionEffect fx : effects) {
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

    /**
     * Generic classifier. Works for any action whose YAML defines outcomes.
     * Looks at the chosen roll's first effect: positive means GAIN, negative
     * means LOSS, no roll at all means SUCCESS.
     */
    private ActionOutcome classifyOutcome(ActionRoll chosen) {
        if (chosen == null) {
            return ActionOutcome.SUCCESS;
        }
        List<ActionEffect> effects = chosen.getEffects();
        if (effects == null || effects.isEmpty()) {
            return ActionOutcome.SUCCESS;
        }
        ActionEffect first = effects.getFirst();
        if (first.isPositive()) {
            return ActionOutcome.GAIN;
        }
        return ActionOutcome.LOSS;
    }
}
