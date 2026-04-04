package com.pcunha.svt.domain.port;

import com.pcunha.svt.domain.model.LeaderboardEntry;

import java.util.List;

public interface LeaderboardPort {
    void save(LeaderboardEntry leaderboardEntry);

    List<LeaderboardEntry> getTopScores();
}
