package com.pcunha.svt.infrastructure.api;

import com.pcunha.svt.domain.model.LeaderboardEntry;
import com.pcunha.svt.domain.port.LeaderboardPort;
import com.pcunha.svt.infrastructure.persistence.LeaderboardRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class LeaderboardAdapter implements LeaderboardPort {
    private final LeaderboardRepository leaderboardRepository;

    public LeaderboardAdapter(LeaderboardRepository repository) {
        this.leaderboardRepository = repository;
    }

    @Override
    public void save(LeaderboardEntry entry) {
        leaderboardRepository.save(entry);
    }

    @Override
    public List<LeaderboardEntry> getTopScores() {
        return leaderboardRepository.findTop10ByDailyRunFalseOrderByWeightedScoreDesc();
    }

    @Override
    public List<LeaderboardEntry> getDailyTopScores(LocalDateTime start, LocalDateTime end) {
        return leaderboardRepository.findTop10ByDailyRunTrueAndCreatedAtBetweenOrderByWeightedScoreDesc(start, end);
    }

    @Override
    public void deleteExpiredDailyScores(LocalDateTime cutoff) {
        leaderboardRepository.deleteByDailyRunTrueAndCreatedAtBefore(cutoff);
    }
}
