package com.pcunha.svt.infrastructure.api;

import com.pcunha.svt.domain.WeatherCategory;
import com.pcunha.svt.domain.model.Location;
import com.pcunha.svt.domain.model.WeatherSignal;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MockWeatherAdapterTest {
    private final Location sanJose = new Location("San Jose", 37.3382, -121.8863);

    @Test
    void returnsValidWeatherCategory() {
        MockWeatherAdapter adapter = new MockWeatherAdapter(new Random(42));
        WeatherSignal signal = adapter.getWeather(sanJose);
        assertNotNull(signal.weatherCategory());
    }

    @Test
    void returnsTemperatureBetween10And45() {
        MockWeatherAdapter adapter = new MockWeatherAdapter(new Random(42));
        for (int i = 0; i < 100; i++) {
            WeatherSignal signal = adapter.getWeather(sanJose);
            assertTrue(signal.temperature() >= 10, "Temperature should be >= 10");
            assertTrue(signal.temperature() <= 45, "Temperature should be <= 45");
        }
    }

    @Test
    void deterministicWithSameSeed() {
        WeatherSignal first = new MockWeatherAdapter(new Random(1)).getWeather(sanJose);
        WeatherSignal second = new MockWeatherAdapter(new Random(1)).getWeather(sanJose);
        assertEquals(first.weatherCategory(), second.weatherCategory());
        assertEquals(first.temperature(), second.temperature());
    }

    @Test
    void producesVariedWeatherAcrossManyCalls() {
        MockWeatherAdapter adapter = new MockWeatherAdapter(new Random(0));
        Set<WeatherCategory> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            seen.add(adapter.getWeather(sanJose).weatherCategory());
        }
        assertTrue(seen.size() > 1, "Should produce more than one weather category");
    }
}
