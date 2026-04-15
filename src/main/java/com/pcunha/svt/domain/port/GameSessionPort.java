package com.pcunha.svt.domain.port;

import com.pcunha.svt.domain.model.GameSession;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GameSessionPort {
    GameSession save(GameSession session);

    Optional<GameSession> findActiveByUserId(String userId);

    Optional<GameSession> findActiveByPlayerToken(String playerToken);

    List<GameSession> findActiveSessionsByUserId(String userId);

    List<GameSession> findActiveSessionsByPlayerToken(String playerToken);

    Optional<GameSession> findByRoomAndUser(String roomId, String userId);

    Optional<GameSession> findByRoomAndPlayer(String roomId, String playerToken);

    void deleteByRoomIds(Collection<String> roomIds);
}
