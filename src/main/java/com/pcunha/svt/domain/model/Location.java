package com.pcunha.svt.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Location {
    private String name;
    private double latitude;
    private double longitude;
}
