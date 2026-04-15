package com.pcunha.svt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One player's slot inside a Room. Stores the serialized GameState.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class GameSession {
    @Id
    private String id;

    @Column(nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String playerToken;

    private String userId;

    private String playerName;

    @Column(columnDefinition = "TEXT")
    private String gameStateData;

    private boolean completed;

    private LocalDateTime createdAt;

    private LocalDateTime lastActionAt;

    @Version
    private long version;
}
