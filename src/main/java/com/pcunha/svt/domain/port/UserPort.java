package com.pcunha.svt.domain.port;

import com.pcunha.svt.domain.model.UserAccount;

import java.util.Optional;

public interface UserPort {
    UserAccount save(UserAccount user);

    Optional<UserAccount> findById(String id);

    Optional<UserAccount> findByLogin(String login);
}
