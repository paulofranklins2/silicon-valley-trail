package com.pcunha.svt.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Location {
    private String name;
    private double latitude;
    private double longitude;
}
