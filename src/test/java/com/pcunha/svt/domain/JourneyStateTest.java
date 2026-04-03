package com.pcunha.svt.domain;

import com.pcunha.svt.domain.model.JourneyState;
import com.pcunha.svt.domain.model.Location;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JourneyStateTest {

    private JourneyState simpleJourney() {
        List<Location> locations = List.of(
                new Location("A", 0, 0),
                new Location("B", 0, 0),
                new Location("C", 0, 0)
        );
        // A -> B is 10, B -> C is 20 total = 30
        List<Double> distances = List.of(10.0, 20.0);
        return new JourneyState(locations, distances);
    }


    @Test
    public void reducingDistance() {
        JourneyState journeyState = simpleJourney(); // total 30
        assertEquals(0, journeyState.getCurrentLocationIndex()); // should be at A
        journeyState.travel(5);
        assertEquals(5, journeyState.getDistanceToNextLocation()); // 5 left
        assertEquals(0, journeyState.getCurrentLocationIndex()); // still A
    }

    @Test
    public void travelToNextLocation() {
        JourneyState journeyState = simpleJourney(); // total 30
        assertEquals(0, journeyState.getCurrentLocationIndex()); // should be at A
        journeyState.travel(10); // move to B
        assertEquals(1, journeyState.getCurrentLocationIndex()); // now at B
        assertEquals(20, journeyState.getDistanceToNextLocation()); // 20 left
    }

    @Test
    public void leftOverCarryOver() {
        JourneyState journeyState = simpleJourney(); // total 30;
        assertEquals(0, journeyState.getCurrentLocationIndex()); // should be at A
        journeyState.travel(17); // move B + 7
        assertEquals(1, journeyState.getCurrentLocationIndex()); // should be at B still
        assertEquals(13, journeyState.getDistanceToNextLocation()); // 13 left
    }

    @Test
    public void reachFinalDestination() {
        JourneyState journeyState = simpleJourney(); // total 30
        assertEquals(0, journeyState.getCurrentLocationIndex()); // should be at A
        journeyState.travel(10);
        assertEquals(1, journeyState.getCurrentLocationIndex()); // should be at B
        journeyState.travel(5);
        assertEquals(1, journeyState.getCurrentLocationIndex()); // should be at B still with 5 leftover
        assertEquals(15, journeyState.getDistanceToNextLocation()); // 15 missing for next destination
        journeyState.travel(15);
        assertEquals(2, journeyState.getCurrentLocationIndex()); // should be at C
        assertEquals(0, journeyState.getDistanceToNextLocation()); // 0 left
    }

    @Test
    public void travelAfterDestinationDoesNothing() {
        JourneyState journeyState = simpleJourney(); // total 30
        assertEquals(0, journeyState.getCurrentLocationIndex()); // should be at A
        journeyState.travel(10);
        assertEquals(1, journeyState.getCurrentLocationIndex()); // should be at B
        journeyState.travel(20);
        assertEquals(2, journeyState.getCurrentLocationIndex()); // should be at C
        journeyState.travel(100);
        assertEquals(2, journeyState.getCurrentLocationIndex()); // should be at C
        assertEquals(0, journeyState.getDistanceToNextLocation()); // 0 left
    }

    @Test
    public void travelThroughMultipleLocations() {
        List<Location> locations = List.of(
                new Location("A", 0, 0),
                new Location("B", 0, 0),
                new Location("C", 0, 0),
                new Location("D", 0, 0)
        );
        // A to B is 3, B to C is 2, C to D is 10
        List<Double> distances = List.of(3.0, 2.0, 10.0);
        JourneyState journey = new JourneyState(locations, distances); // total 15

        journey.travel(6); // passes through A to B (3) and B to C (2), 1 leftover into C to D
        assertEquals(2, journey.getCurrentLocationIndex()); // at C
        assertEquals(9, journey.getDistanceToNextLocation()); // 10 - 1
    }
}