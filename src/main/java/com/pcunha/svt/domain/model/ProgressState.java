package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.WeatherCategory;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ProgressState {
    private int turn = 1;
    @Setter
    private WeatherCategory currentWeather;
    @Setter
    private double currentWeatherTemperature;
    @Setter
    private GameEvent pendingEvent;

    public void nextTurn() {
        turn++;
    }

    public void clearPendingEvent() {
        this.pendingEvent = null;
    }
}
