package com.pcunha.svt.infrastructure.persistence;

import com.pcunha.svt.domain.model.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {
}
