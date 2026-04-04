package com.pcunha.svt.infrastructure.api;

import com.pcunha.svt.domain.WeatherCategory;
import com.pcunha.svt.domain.model.Location;
import com.pcunha.svt.domain.model.WeatherSignal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DemoWeatherAdapterTest {
    private final Location sanJose = new Location("San Jose", 37.3382, -121.8863);

    @Test
    void cyclesThroughAllWeatherTypes() {
        DemoWeatherAdapter adapter = new DemoWeatherAdapter();
        WeatherCategory[] expected = WeatherCategory.values();

        for (WeatherCategory category : expected) {
            WeatherSignal signal = adapter.getWeather(sanJose);
            assertEquals(category, signal.weatherCategory());
        }
    }

    @Test
    void wrapsAroundAfterFullCycle() {
        DemoWeatherAdapter adapter = new DemoWeatherAdapter();
        int totalCategories = WeatherCategory.values().length;

        // exhaust one full cycle
        for (int i = 0; i < totalCategories; i++) {
            adapter.getWeather(sanJose);
        }

        // next call should restart from the first category
        WeatherSignal signal = adapter.getWeather(sanJose);
        assertEquals(WeatherCategory.values()[0], signal.weatherCategory());
    }

    @Test
    void returnsMatchingTemperatureForCategory() {
        DemoWeatherAdapter adapter = new DemoWeatherAdapter();

        assertEquals(22.0, adapter.getWeather(sanJose).temperature()); // CLEAR
        assertEquals(14.0, adapter.getWeather(sanJose).temperature()); // RAINY
        assertEquals(10.0, adapter.getWeather(sanJose).temperature()); // STORMY
        assertEquals(38.0, adapter.getWeather(sanJose).temperature()); // HEATWAVE
    }
}
