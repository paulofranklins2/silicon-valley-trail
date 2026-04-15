package com.pcunha.svt.domain.port;

import com.pcunha.svt.domain.model.GameSession;

import java.util.Collection;
import java.util.Optional;

public interface GameSessionPort {
    GameSession save(GameSession session);

    Optional<GameSession> findActiveByPlayerToken(String playerToken);

    Optional<GameSession> findByRoomAndPlayer(String roomId, String playerToken);

    void deleteByRoomIds(Collection<String> roomIds);
}
