package com.pcunha.svt.domain.port;

import com.pcunha.svt.domain.model.Location;
import com.pcunha.svt.domain.model.WeatherSignal;

public interface WeatherPort {
    WeatherSignal getWeather(Location location);
}
