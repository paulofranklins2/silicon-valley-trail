package com.pcunha.svt.domain.port;

import com.pcunha.svt.domain.model.AuthSession;

import java.util.Optional;

public interface AuthSessionPort {
    AuthSession save(AuthSession session);

    Optional<AuthSession> findById(String id);

    void deleteById(String id);
}
