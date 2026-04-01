package com.pcunha.svt.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class EventOutcome {
    private final String description;
    private final int healthChange;
    private final int energyChange;
    private final int moraleChange;
    private final int cashChange;
    private final int foodChange;
    private final int computeCreditsChange;
}
