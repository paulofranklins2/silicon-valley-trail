package com.pcunha.svt.domain.port;

import com.pcunha.svt.domain.Location;
import com.pcunha.svt.domain.WeatherSignal;

public interface WeatherPort {
    WeatherSignal getWeather(Location location);
}
