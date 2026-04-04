package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.StatType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ActionInfo {
    private GameAction action;
    private String displayName;
    private double travelDistance;
    private double computePenaltyFactor;
    private List<Effect> effects;

    public int getEnergyCost() {
        return effects.stream()
                .filter(e -> e.stat == StatType.ENERGY && e.value < 0 && !e.random)
                .mapToInt(e -> Math.abs(e.value))
                .findFirst()
                .orElse(0);
    }

    public int getEffect(StatType stat) {
        return effects.stream()
                .filter(e -> e.stat == stat && !e.random)
                .mapToInt(e -> e.value)
                .findFirst()
                .orElse(0);
    }

    public int getRandomEffect(StatType stat) {
        return effects.stream()
                .filter(e -> e.stat == stat && e.random)
                .mapToInt(e -> e.value)
                .findFirst()
                .orElse(0);
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Effect {
        private StatType stat;
        private int value;
        private boolean random;

        public boolean isPositive() {
            return value > 0;
        }
    }
}
