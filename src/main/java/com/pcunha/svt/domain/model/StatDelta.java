package com.pcunha.svt.domain.model;

/**
 * Group of stat changes applied to the player.
 */
public record StatDelta(int health, int energy, int morale, int food) {
}
