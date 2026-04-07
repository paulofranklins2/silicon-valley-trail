package com.pcunha.svt.application;

import com.pcunha.svt.domain.model.StatDelta;

/**
 * Game balance config.
 */
public record Tunables(
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
