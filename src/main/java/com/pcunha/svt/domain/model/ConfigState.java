package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.GameMode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ConfigState implements Serializable {
    private GameMode gameMode = GameMode.EASY;
    private GameMode requestedGameMode;
    private boolean usedFallbackDistances;
}
