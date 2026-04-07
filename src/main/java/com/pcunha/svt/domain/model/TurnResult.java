package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.ActionOutcome;
import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.WeatherCategory;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TurnResult {
    private GameAction gameAction;
    private ActionOutcome actionOutcome;
    private GameEvent gameEvent;
    private WeatherCategory weatherCategory;
    private double weatherTemperature;
    private boolean waitingEventChoice;

}
