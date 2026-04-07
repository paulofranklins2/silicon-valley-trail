package com.pcunha.svt.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LossReason {
    STARVATION("Starved"),
    POOR_HEALTH("Collapsed"),
    POOR_MORALE("Morale Broke"),
    NO_CASH("Bankrupt");

    private final String displayName;
}
