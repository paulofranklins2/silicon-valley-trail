package com.pcunha.svt.domain;

import lombok.Getter;

import java.util.List;

@Getter
public class JourneyState {
    private final List<Location> locations;
    private final List<Double> distances;
    private int currentLocationIndex;
    private double distanceToNextLocation;

    public JourneyState(List<Location> locations, List<Double> distances) {
        this.locations = locations;
        this.distances = distances;
        this.currentLocationIndex = 0;
        this.distanceToNextLocation = distances.getFirst();
    }

    public void travel(double distance) {
        if (hasReachedDestination()) return;

        distanceToNextLocation -= distance;
        if (distanceToNextLocation <= 0 && hasNextLocation()) {
            // carry unused travel distance to next location
            double leftOver = Math.abs(distanceToNextLocation);
            currentLocationIndex++;
            if (hasNextLocation()) {
                distanceToNextLocation = distances.get(currentLocationIndex) - leftOver;
            } else {
                distanceToNextLocation = 0;
            }
        }
    }

    public boolean hasReachedDestination() {
        return !hasNextLocation();
    }

    public boolean hasNextLocation() {
        return currentLocationIndex < locations.size() - 1;
    }

    public Location getCurrentLocation() {
        return locations.get(currentLocationIndex);
    }
}
