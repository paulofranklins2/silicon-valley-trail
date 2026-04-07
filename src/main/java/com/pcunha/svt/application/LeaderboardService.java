package com.pcunha.svt.application;

import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.LeaderboardEntry;
import com.pcunha.svt.domain.model.SubmissionResult;
import com.pcunha.svt.domain.port.LeaderboardPort;

public class LeaderboardService {
    private final LeaderboardPort leaderboardPort;

    public LeaderboardService(LeaderboardPort leaderboardPort) {
        this.leaderboardPort = leaderboardPort;
    }

    public SubmissionResult submitResult(GameState gameState, String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return SubmissionResult.error("Name required");
        }
        if (gameState.isLeaderboardSubmitted()) {
            return SubmissionResult.error("Already submitted");
        }
        LeaderboardEntry leaderboardEntry = LeaderboardEntry.fromGameState(gameState, playerName.trim());
        leaderboardPort.save(leaderboardEntry);
        gameState.markLeaderboardSubmitted();
        return SubmissionResult.success();
    }
}