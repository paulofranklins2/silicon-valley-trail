package com.pcunha.svt.infrastructure.api;

import com.pcunha.svt.domain.WeatherCategory;
import com.pcunha.svt.domain.model.Location;
import com.pcunha.svt.domain.model.WeatherSignal;
import com.pcunha.svt.domain.port.WeatherPort;

import java.util.Random;

public class MockWeatherAdapter implements WeatherPort {
    private final Random random;
    private final WeatherCategory[] weatherCategories = WeatherCategory.values();

    public MockWeatherAdapter(Random random) {
        this.random = random;
    }

    @Override
    public WeatherSignal getWeather(Location location) {
        WeatherCategory weatherCategory = weatherCategories[random.nextInt(weatherCategories.length)];
        double temperature = 10 + random.nextDouble() * 35;
        return new WeatherSignal(weatherCategory, temperature);
    }
}
