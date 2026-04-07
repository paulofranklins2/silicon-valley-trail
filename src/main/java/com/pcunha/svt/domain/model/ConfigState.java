package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.GameMode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfigState {
    private GameMode gameMode = GameMode.EASY;
    private GameMode requestedGameMode;
    private boolean usedFallbackDistances;
}
