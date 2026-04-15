package com.pcunha.svt.infrastructure.persistence;

import com.pcunha.svt.domain.model.UserAccount;
import com.pcunha.svt.domain.port.UserPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserAdapter implements UserPort {
    private final UserRepository repository;

    public UserAdapter(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserAccount save(UserAccount user) {
        return repository.save(user);
    }

    @Override
    public Optional<UserAccount> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public Optional<UserAccount> findByLogin(String login) {
        return repository.findByLogin(login);
    }
}
