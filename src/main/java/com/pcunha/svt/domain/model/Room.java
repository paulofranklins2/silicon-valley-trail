package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.RoomStatus;
import com.pcunha.svt.domain.RoomType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Container for one or more players sharing a game context.
 * Daily rooms key on (mode, dateBucket); solo rooms leave dateBucket null.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Room {
    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private RoomType type;

    @Enumerated(EnumType.STRING)
    private GameMode mode;

    private long seed;

    @Enumerated(EnumType.STRING)
    private RoomStatus status;

    private LocalDateTime createdAt;

    private LocalDate dateBucket;
}
