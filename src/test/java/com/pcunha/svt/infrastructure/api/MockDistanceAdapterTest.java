package com.pcunha.svt.infrastructure.api;

import com.pcunha.svt.domain.model.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MockDistanceAdapterTest {
    private final MockDistanceAdapter adapter = new MockDistanceAdapter();

    @Test
    void returnsFixedFallbackDistance() {
        Location a = new Location("San Jose", 37.3382, -121.8863);
        Location b = new Location("Santa Clara", 37.3541, -121.9552);
        assertEquals(10.0, adapter.getDistance(a, b));
    }

    @Test
    void returnsSameDistanceRegardlessOfLocations() {
        Location a = new Location("A", 0.0, 0.0);
        Location b = new Location("B", 90.0, 180.0);
        Location c = new Location("C", -45.0, -90.0);
        assertEquals(adapter.getDistance(a, b), adapter.getDistance(a, c));
        assertEquals(adapter.getDistance(a, b), adapter.getDistance(b, c));
    }
}
