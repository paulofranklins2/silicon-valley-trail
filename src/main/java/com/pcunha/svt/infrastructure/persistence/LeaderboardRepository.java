package com.pcunha.svt.infrastructure.persistence;

import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.model.LeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {
    List<LeaderboardEntry> findTop10ByGameModeOrderByScoreDesc(GameMode gameMode);
}
