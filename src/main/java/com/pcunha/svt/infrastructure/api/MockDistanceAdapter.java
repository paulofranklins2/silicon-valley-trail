package com.pcunha.svt.infrastructure.api;

import com.pcunha.svt.domain.model.Location;
import com.pcunha.svt.domain.port.DistancePort;

public class MockDistanceAdapter implements DistancePort {
    private static final double FALLBACK_DISTANCE = 10.0;

    @Override
    public double getDistance(Location location1, Location location2) {
        // fallback with 10km for any 2 distance
        return FALLBACK_DISTANCE;
    }
}
