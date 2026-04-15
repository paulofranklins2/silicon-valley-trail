package com.pcunha.svt.infrastructure.persistence;

import com.pcunha.svt.domain.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, String> {
    Optional<UserAccount> findByLogin(String login);
}
