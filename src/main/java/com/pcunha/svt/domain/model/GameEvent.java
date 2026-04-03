package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.EventCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class GameEvent {
    private final String title;
    private final String description;
    private final EventCategory eventCategory;
    private final int healthChange;
    private final int energyChange;
    private final int moraleChange;
    private final int cashChange;
    private final int foodChange;
    private final int computeCreditsChange;
    private final List<EventOutcome> choices;


}
