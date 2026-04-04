package com.pcunha.svt.infrastructure.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcunha.svt.domain.model.Location;
import com.pcunha.svt.domain.port.DistancePort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

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
            String url = String.format(
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
            return fallback.getDistance(from, to);
        }
    }
}
