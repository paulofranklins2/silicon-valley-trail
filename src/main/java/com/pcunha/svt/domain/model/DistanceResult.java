package com.pcunha.svt.domain.model;

import java.util.List;

public record DistanceResult(List<Double> distances, boolean usedFallback) {
}
