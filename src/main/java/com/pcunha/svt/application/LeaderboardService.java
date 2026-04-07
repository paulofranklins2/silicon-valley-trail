package com.pcunha.svt.application;

import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.LeaderboardEntry;
import com.pcunha.svt.domain.model.SubmissionResult;
import com.pcunha.svt.domain.port.LeaderboardPort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LeaderboardService {
    private final LeaderboardPort leaderboardPort;

    public LeaderboardService(LeaderboardPort leaderboardPort) {
        this.leaderboardPort = leaderboardPort;
    }

    public SubmissionResult submitResult(GameState gameState, String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return SubmissionResult.error("Name required");
        }
        if (gameState.getEndingState().isLeaderboardSubmitted()) {
            return SubmissionResult.error("Already submitted");
        }
        LeaderboardEntry leaderboardEntry = LeaderboardEntry.fromGameState(gameState, playerName.trim());
        leaderboardPort.save(leaderboardEntry);
        gameState.getEndingState().markLeaderboardSubmitted();
        return SubmissionResult.success();
    }

    public Map<GameMode, List<LeaderboardEntry>> getTopScoresByMode() {
        Map<GameMode, List<LeaderboardEntry>> scores = new LinkedHashMap<>();
        for (GameMode mode : GameMode.values()) {
            scores.put(mode, leaderboardPort.getTopScores(mode));
        }
        return scores;
    }
}