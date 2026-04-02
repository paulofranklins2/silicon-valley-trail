package com.pcunha.svt.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WeatherSignal {
    private final WeatherCategory weatherCategory;
    private final double temperature;
}
