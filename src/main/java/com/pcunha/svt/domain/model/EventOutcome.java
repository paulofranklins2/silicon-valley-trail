package com.pcunha.svt.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventOutcome {
    private String description;
    private int healthChange;
    private int energyChange;
    private int moraleChange;
    private int cashChange;
    private int foodChange;
    private int computeCreditsChange;
}
