package com.pcunha.svt.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Location implements Serializable {
    private String name;
    private double latitude;
    private double longitude;
}
