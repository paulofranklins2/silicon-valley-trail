package com.pcunha.svt.infrastructure.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcunha.svt.domain.model.Location;
import com.pcunha.svt.domain.WeatherCategory;
import com.pcunha.svt.domain.model.WeatherSignal;
import com.pcunha.svt.domain.port.WeatherPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

public class OpenMeteoAdapter implements WeatherPort {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final MockWeatherAdapter mockWeatherAdapter;

    public OpenMeteoAdapter(MockWeatherAdapter mockWeatherAdapter) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.objectMapper = new ObjectMapper();
        this.mockWeatherAdapter = mockWeatherAdapter;
    }

    @Override
    public WeatherSignal getWeather(Location location) {
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
            return new WeatherSignal(weatherCategory, temperature);
        } catch (Exception e) {
            return mockWeatherAdapter.getWeather(location);
        }
    }

    private WeatherCategory mapWeatherCode(int code) {
        if (code <= 3) return WeatherCategory.CLEAR;
        if (code <= 67) return WeatherCategory.RAINY;
        if (code <= 82) return WeatherCategory.STORMY;
        if (code >= 95) return WeatherCategory.STORMY;
        return WeatherCategory.HEATWAVE;
    }

}
