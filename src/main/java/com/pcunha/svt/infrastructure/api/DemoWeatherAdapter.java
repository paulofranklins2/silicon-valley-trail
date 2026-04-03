package com.pcunha.svt.infrastructure.api;

import com.pcunha.svt.domain.WeatherCategory;
import com.pcunha.svt.domain.model.Location;
import com.pcunha.svt.domain.model.WeatherSignal;
import com.pcunha.svt.domain.port.WeatherPort;

/**
 * Cycles through all weather types so every condition is shown during a playthrough.
 * Used for demos and testing the UI.
 */
public class DemoWeatherAdapter implements WeatherPort {
    private final WeatherCategory[] categories = WeatherCategory.values();
    private int index = 0;

    @Override
    public WeatherSignal getWeather(Location location) {
        WeatherCategory category = categories[index % categories.length];
        index++;
        return new WeatherSignal(category, temperatureFor(category));
    }

    private double temperatureFor(WeatherCategory category) {
        return switch (category) {
            case CLEAR -> 22.0;
            case RAINY -> 14.0;
            case STORMY -> 10.0;
            case HEATWAVE -> 38.0;
        };
    }
}
