package com.pcunha.svt.infrastructure.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcunha.svt.domain.model.DistanceResult;
import com.pcunha.svt.domain.model.Location;
import com.pcunha.svt.domain.port.DistancePort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OpenRouteServiceAdapter implements DistancePort {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DistancePort fallback;
    private final String apiKey;
    private final String profile;

    public OpenRouteServiceAdapter(DistancePort fallback, String apiKey, String profile) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.fallback = fallback;
        this.apiKey = apiKey;
        this.profile = profile;
    }

    @Override
    public double getDistance(Location from, Location to) {
        return fallback.getDistance(from, to);
    }

    @Override
    public DistanceResult calculateLegDistances(List<Location> locations) {
        if (apiKey == null || apiKey.isBlank()) {
            return new DistanceResult(fallback.calculateLegDistances(locations).distances(), true);
        }

        try {
            StringBuilder coords = new StringBuilder("[");
            for (int i = 0; i < locations.size(); i++) {
                if (i > 0) coords.append(',');
                coords.append(String.format(Locale.US, "[%f,%f]",
                        locations.get(i).getLongitude(), locations.get(i).getLatitude()));
            }
            coords.append(']');

            String body = "{\"coordinates\":" + coords + "}";
            String url = "https://api.openrouteservice.org/v2/directions/" + profile + "/json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode segments = root.get("routes").get(0).get("segments");

            List<Double> distances = new ArrayList<>();
            for (int i = 0; i < segments.size(); i++) {
                distances.add(segments.get(i).get("distance").asDouble() / 1000.0);
            }
            return new DistanceResult(distances, false);
        } catch (Exception e) {
            System.err.println("OpenRouteService (" + profile + ") unavailable: " + e.getMessage());
            return new DistanceResult(fallback.calculateLegDistances(locations).distances(), true);
        }
    }
}
