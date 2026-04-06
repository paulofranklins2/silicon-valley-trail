package com.pcunha.svt.infrastructure.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcunha.svt.domain.WeatherCategory;
import com.pcunha.svt.domain.model.Location;
import com.pcunha.svt.domain.model.WeatherSignal;
import com.pcunha.svt.domain.port.WeatherPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OpenMeteoAdapter implements WeatherPort {
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final MockWeatherAdapter mockWeatherAdapter;
    private final Map<String, CachedWeather> cache = new ConcurrentHashMap<>();

    public OpenMeteoAdapter(MockWeatherAdapter mockWeatherAdapter) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.objectMapper = new ObjectMapper();
        this.mockWeatherAdapter = mockWeatherAdapter;
    }

    public void preloadWeather(List<Location> locations) {
        System.out.println("Caching weather for " + locations.size() + " cities...");
        for (Location location : locations) {
            getWeather(location);
        }
        System.out.println("Weather: cached");
    }

    @Override
    public WeatherSignal getWeather(Location location) {
        String cacheKey = String.format(Locale.US, "%.2f,%.2f", location.getLatitude(), location.getLongitude());

        CachedWeather cached = cache.get(cacheKey);
        if (cached != null && cached.isValid()) {
            return cached.signal;
        }

        try {
            String url = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true",
                    location.getLatitude(), location.getLongitude()
            );

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            JsonNode root = objectMapper.readTree(httpResponse.body());
            JsonNode currentWeather = root.get("current_weather");
            double temperature = currentWeather.get("temperature").asDouble();
            int weatherCode = currentWeather.get("weathercode").asInt();

            WeatherCategory weatherCategory = mapWeatherCode(weatherCode);
            WeatherSignal signal = new WeatherSignal(weatherCategory, temperature);

            cache.put(cacheKey, new CachedWeather(signal, Instant.now()));
            return signal;
        } catch (Exception e) {
            return mockWeatherAdapter.getWeather(location);
        }
    }

    // WMO weather interpretation codes: https://open-meteo.com/en/docs
    private WeatherCategory mapWeatherCode(int code) {
        if (code <= 3) return WeatherCategory.CLEAR;
        if (code <= 67) return WeatherCategory.RAINY;
        if (code <= 82) return WeatherCategory.STORMY;
        if (code >= 95) return WeatherCategory.STORMY;
        return WeatherCategory.HEATWAVE;
    }

    private record CachedWeather(WeatherSignal signal, Instant fetchedAt) {
        boolean isValid() {
            return Instant.now().isBefore(fetchedAt.plus(CACHE_TTL));
        }
    }
}
