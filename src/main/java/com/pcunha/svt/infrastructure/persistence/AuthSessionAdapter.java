package com.pcunha.svt.infrastructure.persistence;

import com.pcunha.svt.domain.model.AuthSession;
import com.pcunha.svt.domain.port.AuthSessionPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthSessionAdapter implements AuthSessionPort {
    private final AuthSessionRepository repository;

    public AuthSessionAdapter(AuthSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuthSession save(AuthSession session) {
        return repository.save(session);
    }

    @Override
    public Optional<AuthSession> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
