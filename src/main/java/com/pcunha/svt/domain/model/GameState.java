package com.pcunha.svt.domain.model;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GameState {
    private final String teamName;
    private final TeamState teamState;
    private final ResourceState resourceState;
    private final JourneyState journeyState;
    private final EndingState endingState = new EndingState();
    private final ResourceGraceState graceState = new ResourceGraceState();
    private final MarketState marketState = new MarketState();
    private final ConfigState configState = new ConfigState();
    private final ProgressState progressState = new ProgressState();

    public GameState(TeamState teamState, ResourceState resourceState, JourneyState journeyState, String teamName) {
        this.teamName = teamName;
        this.teamState = teamState;
        this.resourceState = resourceState;
        this.journeyState = journeyState;
    }
}
