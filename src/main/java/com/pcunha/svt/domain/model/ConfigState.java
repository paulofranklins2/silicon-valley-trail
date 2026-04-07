package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.GameMode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfigState {
    private GameMode gameMode = GameMode.FAST;
    private GameMode requestedGameMode;
    private boolean usedFallbackDistances;
}
