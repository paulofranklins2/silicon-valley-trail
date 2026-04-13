package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.WeatherCategory;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
public class ProgressState implements Serializable {
    private int turn = 1;
    @Setter
    private WeatherCategory currentWeather;
    @Setter
    private double currentWeatherTemperature;
    @Setter
    private GameEvent pendingEvent;
    @Setter
    private long startTimeMs;
    @Setter
    private long endTimeMs;

    public void nextTurn() {
        turn++;
    }

    public void clearPendingEvent() {
        this.pendingEvent = null;
    }

    public String getElapsedTime() {
        long end = endTimeMs > 0 ? endTimeMs : System.currentTimeMillis();
        if (startTimeMs == 0 || end < startTimeMs) {
            return "0:00";
        }
        return TimeFormatter.format(end - startTimeMs);
    }
}
