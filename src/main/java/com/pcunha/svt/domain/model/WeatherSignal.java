package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.WeatherCategory;
import lombok.AllArgsConstructor;

public record WeatherSignal(WeatherCategory weatherCategory, double temperature) {
}
