package com.pcunha.svt.infrastructure.api;

import com.pcunha.svt.domain.model.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OsrmDistanceAdapterTest {
    private final HaversineDistanceAdapter haversine = new HaversineDistanceAdapter();
    private final OsrmDistanceAdapter adapter = new OsrmDistanceAdapter(haversine);

    @Test
    void returnsValidDistanceForRealLocations() {
        Location sanJose = new Location("San Jose", 37.3382, -121.8863);
        Location santaClara = new Location("Santa Clara", 37.3541, -121.9552);
        double distance = adapter.getDistance(sanJose, santaClara);

        // real driving or haversine fallback, either way should be reasonable
        assertTrue(distance > 3 && distance < 20,
                "Distance should be between 3-20 km, got: " + distance);
    }

    @Test
    void fallsBackToHaversineOnInvalidCoordinates() {
        Location invalid1 = new Location("Nowhere", 999.0, 999.0);
        Location invalid2 = new Location("Also Nowhere", -999.0, -999.0);
        double distance = adapter.getDistance(invalid1, invalid2);

        // should not throw, should return haversine fallback
        assertTrue(distance >= 0, "Fallback should return a non-negative distance");
    }

    @Test
    void roadDistanceIsLongerThanStraightLine() {
        Location sanJose = new Location("San Jose", 37.3382, -121.8863);
        Location sanFrancisco = new Location("San Francisco", 37.7749, -122.4194);

        double roadDistance = adapter.getDistance(sanJose, sanFrancisco);
        double straightDistance = haversine.getDistance(sanJose, sanFrancisco);

        // road should be longer than straight line (or equal if fallback triggered)
        assertTrue(roadDistance >= straightDistance,
                "Road (" + roadDistance + " km) should be >= straight line (" + straightDistance + " km)");
    }
}
