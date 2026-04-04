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

public class OsrmDistanceAdapter implements DistancePort {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DistancePort fallback;

    public OsrmDistanceAdapter(DistancePort fallback) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
        this.fallback = fallback;
    }

    @Override
    public double getDistance(Location from, Location to) {
        try {
            String url = String.format(Locale.US,
                    "https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=false",
                    from.getLongitude(), from.getLatitude(),
                    to.getLongitude(), to.getLatitude()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            double meters = root.get("routes").get(0).get("distance").asDouble();
            return meters / 1000.0;
        } catch (Exception e) {
            System.err.println("OSRM unavailable, falling back to estimated distances: " + e.getMessage());
            return fallback.getDistance(from, to);
        }
    }

    /**
     * Batch: calculates all leg distances in a single OSRM request.
     * Falls back to Haversine on failure.
     */
    @Override
    public DistanceResult calculateLegDistances(List<Location> locations) {
        try {
            StringBuilder coords = new StringBuilder();
            for (int i = 0; i < locations.size(); i++) {
                if (i > 0) coords.append(';');
                coords.append(String.format(Locale.US, "%f,%f",
                        locations.get(i).getLongitude(), locations.get(i).getLatitude()));
            }

            String url = "https://router.project-osrm.org/route/v1/driving/"
                    + coords + "?overview=false&steps=false";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode legs = root.get("routes").get(0).get("legs");

            List<Double> distances = new ArrayList<>();
            for (int i = 0; i < legs.size(); i++) {
                distances.add(legs.get(i).get("distance").asDouble() / 1000.0);
            }
            return new DistanceResult(distances, false);
        } catch (Exception e) {
            System.err.println("OSRM unavailable, falling back to estimated distances: " + e.getMessage());
            return new DistanceResult(fallback.calculateLegDistances(locations).distances(), true);
        }
    }
}
