package com.pcunha.svt.application;

import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.LeaderboardEntry;
import com.pcunha.svt.domain.model.SubmissionResult;
import com.pcunha.svt.domain.port.LeaderboardPort;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LeaderboardService {
    private static final int MAX_PLAYER_NAME_LENGTH = 10;

    private final LeaderboardPort leaderboardPort;
    private final ScoreCalculator scoreCalculator;

    public LeaderboardService(LeaderboardPort leaderboardPort, ScoreCalculator scoreCalculator) {
        this.leaderboardPort = leaderboardPort;
        this.scoreCalculator = scoreCalculator;
    }

    public SubmissionResult submitResult(GameState gameState, String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return SubmissionResult.error("Name required");
        }
        String trimmed = playerName.trim();
        if (trimmed.length() > MAX_PLAYER_NAME_LENGTH) {
            return SubmissionResult.error("Name too long (max " + MAX_PLAYER_NAME_LENGTH + " characters)");
        }
        if (gameState.getEndingState().isLeaderboardSubmitted()) {
            return SubmissionResult.error("Already submitted");
        }
        LeaderboardEntry entry = buildEntry(gameState, trimmed);
        leaderboardPort.save(entry);
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

    /**
     * Returns the current score for a game without persisting it.
     */
    public int calculateScore(GameState gameState) {
        return scoreCalculator.calculate(gameState);
    }

    /**
     * Builds a LeaderboardEntry from a finished game.
     */
    private LeaderboardEntry buildEntry(GameState gameState, String playerName) {
        LeaderboardEntry leaderboardEntry = new LeaderboardEntry();
        leaderboardEntry.setPlayerName(playerName);
        leaderboardEntry.setTeamName(gameState.getTeamName());
        leaderboardEntry.setTurns(gameState.getProgressState().getTurn());
        leaderboardEntry.setVictory(gameState.getEndingState().isVictory());
        leaderboardEntry.setLastLocation(gameState.getJourneyState().getCurrentLocation().getName());
        leaderboardEntry.setLocationIndex(gameState.getJourneyState().getCurrentLocationIndex());
        leaderboardEntry.setTotalLocations(gameState.getJourneyState().getLocations().size());
        leaderboardEntry.setLossReason(gameState.getEndingState().getLossReason());
        leaderboardEntry.setHealth(gameState.getTeamState().getHealth());
        leaderboardEntry.setEnergy(gameState.getTeamState().getEnergy());
        leaderboardEntry.setMorale(gameState.getTeamState().getMorale());
        leaderboardEntry.setCash(gameState.getResourceState().getCash());
        leaderboardEntry.setFood(gameState.getResourceState().getFood());
        leaderboardEntry.setComputeCredits(gameState.getResourceState().getComputeCredits());
        leaderboardEntry.setScore(scoreCalculator.calculate(gameState));
        leaderboardEntry.setGameMode(gameState.getConfigState().getGameMode());
        leaderboardEntry.setCreatedAt(LocalDateTime.now());
        return leaderboardEntry;
    }
}
