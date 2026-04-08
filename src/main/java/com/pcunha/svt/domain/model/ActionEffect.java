package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.StatType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ActionEffect {
    private StatType stat;
    private int value;

    public boolean isPositive() {
        return value > 0;
    }
}
