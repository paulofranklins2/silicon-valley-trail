package com.pcunha.svt.domain.port;

import com.pcunha.svt.domain.Location;

public interface DistancePort {
    double getDistance(Location location1, Location location2);
}
