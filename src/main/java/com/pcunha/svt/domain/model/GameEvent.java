package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.EventCategory;
import lombok.*;

import java.io.Serializable;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameEvent implements Serializable {
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
