package com.pcunha.svt.infrastructure.api;

import com.pcunha.svt.domain.model.Location;
import com.pcunha.svt.domain.model.WeatherSignal;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenMeteoAdapterTest {
    private final Location sanJose = new Location("San Jose", 37.3382, -121.8863);

    @Test
    void fallsBackToMockOnInvalidLocation() {
        MockWeatherAdapter mock = new MockWeatherAdapter(new Random(42));
        OpenMeteoAdapter adapter = new OpenMeteoAdapter(mock);

        // extreme coordinates that may cause API issues, but should still return a valid signal via fallback
        Location invalid = new Location("Nowhere", 999.0, 999.0);
        WeatherSignal signal = adapter.getWeather(invalid);
        assertNotNull(signal);
        assertNotNull(signal.weatherCategory());
    }

    @Test
    void returnsValidSignalForRealLocation() {
        MockWeatherAdapter mockWeatherAdapter = new MockWeatherAdapter(new Random(42));
        OpenMeteoAdapter openMeteoAdapter = new OpenMeteoAdapter(mockWeatherAdapter);

        // calls API or falls back, either way, result must be valid
        WeatherSignal weatherSignal = openMeteoAdapter.getWeather(sanJose);

        assertNotNull(weatherSignal);
        assertNotNull(weatherSignal.weatherCategory());

        assertTrue(weatherSignal.temperature() > -50 && weatherSignal.temperature() < 60,
                "Temperature should be in a reasonable range");
    }
}
