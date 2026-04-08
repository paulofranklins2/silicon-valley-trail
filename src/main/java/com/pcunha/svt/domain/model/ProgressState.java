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

    public void nextTurn() {
        turn++;
    }

    public void clearPendingEvent() {
        this.pendingEvent = null;
    }
}
