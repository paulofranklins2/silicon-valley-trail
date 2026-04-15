package com.pcunha.svt.infrastructure.persistence;

import com.pcunha.svt.domain.model.GameSession;
import com.pcunha.svt.domain.port.GameSessionPort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

@Component
public class GameSessionAdapter implements GameSessionPort {
    private final GameSessionRepository repository;

    public GameSessionAdapter(GameSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public GameSession save(GameSession session) {
        return repository.save(session);
    }

    @Override
    public Optional<GameSession> findActiveByPlayerToken(String playerToken) {
        return repository.findFirstByPlayerTokenAndCompletedFalseOrderByLastActionAtDesc(playerToken);
    }

    @Override
    public Optional<GameSession> findByRoomAndPlayer(String roomId, String playerToken) {
        return repository.findByRoomIdAndPlayerToken(roomId, playerToken);
    }

    @Override
    public void deleteByRoomIds(Collection<String> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) return;
        repository.deleteByRoomIdIn(roomIds);
    }
}
