package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.EventCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameEvent {
    private String title;
    private String description;
    @Setter
    private EventCategory eventCategory;
    private int healthChange;
    private int energyChange;
    private int moraleChange;
    private int cashChange;
    private int foodChange;
    private int computeCreditsChange;
    private List<EventOutcome> outcomes;


}
