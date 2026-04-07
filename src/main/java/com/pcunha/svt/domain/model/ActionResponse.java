package com.pcunha.svt.domain.model;

/**
 * Turn API Response, Contains the updated state and the result of that turn.
 */
public record ActionResponse(GameState state, TurnResult turnResult) {
}
