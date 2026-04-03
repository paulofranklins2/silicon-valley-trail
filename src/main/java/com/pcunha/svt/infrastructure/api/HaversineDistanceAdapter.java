package com.pcunha.svt.infrastructure.api;

import com.pcunha.svt.domain.model.Location;
import com.pcunha.svt.domain.port.DistancePort;

/**
 * Calculates straight-line distance between two locations using the Haversine formula.
 * Uses Earth's curvature to convert latitude/longitude coordinates into kilometers.
 * No API call needed, pure math.
 */
public class HaversineDistanceAdapter implements DistancePort {
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * @return distance in kilometers between two locations
     */
    @Override
    public double getDistance(Location location1, Location location2) {
        // convert degrees to radians (trig functions need radians)
        double latitude1 = Math.toRadians(location1.getLatitude());
        double haversineAngle = getHaversineAngle(location1, location2, latitude1);

        // central angle: the angle at Earth's center between the two points
        double centralAngle = 2 * Math.atan2(Math.sqrt(haversineAngle), Math.sqrt(1 - haversineAngle));

        // multiply by Earth's radius to get distance in km
        return EARTH_RADIUS_KM * centralAngle;
    }

    private static double getHaversineAngle(Location location1, Location location2, double latitude1) {
        double latitude2 = Math.toRadians(location2.getLatitude());
        double distanceLatitude = Math.toRadians(location2.getLatitude() - location1.getLatitude());
        double distanceLongitude = Math.toRadians(location2.getLongitude() - location1.getLongitude());

        // haversine: how far apart are these two points on a sphere (0 = same spot, 1 = opposite sides)
        return Math.sin(distanceLatitude / 2) * Math.sin(distanceLatitude / 2)
                + Math.cos(latitude1) * Math.cos(latitude2)
                * Math.sin(distanceLongitude / 2) * Math.sin(distanceLongitude / 2);
    }
}
