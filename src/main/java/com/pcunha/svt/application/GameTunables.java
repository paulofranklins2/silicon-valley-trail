package com.pcunha.svt.application;

import com.pcunha.svt.domain.model.StatDelta;

/**
 * Game balance config.
 * Loaded from data/tunables.yaml on startup.
 */
public record GameTunables(
        double eventChance,
        int foodGraceTurns,
        int cashGraceTurns,
        WeatherEffects weather
) {
    public record WeatherEffects(
            StatDelta rainy,
            StatDelta stormy,
            StatDelta heatwave,
            StatDelta clear
    ) {
    }
}
