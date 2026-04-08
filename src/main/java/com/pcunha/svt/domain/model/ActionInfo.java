package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.StatType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ActionInfo {
    private GameAction action;
    private String displayName;
    private double travelDistance;
    private double computePenaltyFactor;
    private List<ActionEffect> effects;
    private List<ActionRoll> outcomes;

    public int getEnergyCost() {
        return effects.stream()
                .filter(e -> e.getStat() == StatType.ENERGY && e.getValue() < 0)
                .mapToInt(e -> Math.abs(e.getValue()))
                .findFirst()
                .orElse(0);
    }

    public boolean hasOutcomes() {
        return outcomes != null && !outcomes.isEmpty();
    }

    public List<ActionRoll> getOutcomes() {
        if (outcomes == null) {
            return Collections.emptyList();
        }
        return outcomes;
    }
}
