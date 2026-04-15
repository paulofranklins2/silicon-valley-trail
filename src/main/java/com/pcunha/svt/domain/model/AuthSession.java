package com.pcunha.svt.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class AuthSession {
    @Id
    private String id;

    private String userId;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;
}
