package com.pcunha.svt.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class UserAccount {
    @Id
    private String id;

    private String login;

    private String passwordHash;

    private LocalDateTime createdAt;
}
