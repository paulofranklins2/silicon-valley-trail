package com.pcunha.svt.infrastructure.persistence;

import com.pcunha.svt.domain.model.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GameSessionRepository extends JpaRepository<GameSession, String> {
    Optional<GameSession> findFirstByUserIdAndCompletedFalseOrderByLastActionAtDesc(String userId);

    Optional<GameSession> findFirstByPlayerTokenAndCompletedFalseOrderByLastActionAtDesc(String playerToken);

    List<GameSession> findAllByUserIdAndCompletedFalseOrderByLastActionAtDesc(String userId);

    List<GameSession> findAllByPlayerTokenAndCompletedFalseOrderByLastActionAtDesc(String playerToken);

    Optional<GameSession> findByRoomIdAndUserId(String roomId, String userId);

    Optional<GameSession> findByRoomIdAndPlayerToken(String roomId, String playerToken);

    void deleteByRoomIdIn(Collection<String> roomIds);
}
