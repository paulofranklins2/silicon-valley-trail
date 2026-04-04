package com.pcunha.svt.domain.port;

import com.pcunha.svt.domain.model.DistanceResult;
import com.pcunha.svt.domain.model.Location;

import java.util.ArrayList;
import java.util.List;

public interface DistancePort {
    double getDistance(Location from, Location to);

    default DistanceResult calculateLegDistances(List<Location> locations) {
        List<Double> distances = new ArrayList<>();
        for (int i = 0; i < locations.size() - 1; i++) {
            distances.add(getDistance(locations.get(i), locations.get(i + 1)));
        }
        return new DistanceResult(distances, false);
    }
}
