package com.pcunha.svt.infrastructure.api;

import com.pcunha.svt.domain.model.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HaversineDistanceAdapterTest {

    @Test
    public void distanceBetweenSanJoseAndSantaClara() {
        HaversineDistanceAdapter adapter = new HaversineDistanceAdapter();
        Location sanJose = new Location("San Jose", 37.3382, -121.8863);
        Location santaClara = new Location("Santa Clara", 37.3541, -121.9552);
        double distance = adapter.getDistance(sanJose, santaClara);
        // should be around 6-8 km
        assertTrue(distance > 5 && distance < 10);
    }

}