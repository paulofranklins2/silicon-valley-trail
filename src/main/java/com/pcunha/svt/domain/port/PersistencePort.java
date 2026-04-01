package com.pcunha.svt.domain.port;

import com.pcunha.svt.domain.GameState;

public interface PersistencePort {
    void save(GameState gameState);

    GameState load(Long id);
}
