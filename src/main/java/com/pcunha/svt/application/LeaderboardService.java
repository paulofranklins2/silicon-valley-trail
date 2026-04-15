package com.pcunha.svt.application;

import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.LeaderboardEntry;
import com.pcunha.svt.domain.model.SubmissionResult;
import com.pcunha.svt.domain.port.LeaderboardPort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class LeaderboardService {
    private static final int MAX_PLAYER_NAME_LENGTH = 10;

    private final LeaderboardPort leaderboardPort;
    private final ScoreCalculator scoreCalculator;

    public LeaderboardService(LeaderboardPort leaderboardPort, ScoreCalculator scoreCalculator) {
        this.leaderboardPort = leaderboardPort;
        this.scoreCalculator = scoreCalculator;
    }

    public SubmissionResult submitResult(GameState gameState, String playerName, boolean dailyRun) {
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
        LeaderboardEntry entry = buildEntry(gameState, trimmed, dailyRun);
        leaderboardPort.save(entry);
        gameState.getEndingState().markLeaderboardSubmitted();
        return SubmissionResult.success();
    }

    public List<LeaderboardEntry> getTopScores() {
        return leaderboardPort.getTopScores();
    }

    public List<LeaderboardEntry> getDailyTopScores() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        leaderboardPort.deleteExpiredDailyScores(start);
        return leaderboardPort.getDailyTopScores(start, end);
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
    private LeaderboardEntry buildEntry(GameState gameState, String playerName, boolean dailyRun) {
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
        long start = gameState.getProgressState().getStartTimeMs();
        long end = gameState.getProgressState().getEndTimeMs();
        if (start > 0 && end > start) {
            leaderboardEntry.setElapsedMs(end - start);
        }
        int rawScore = scoreCalculator.calculate(gameState);
        var mode = gameState.getConfigState().getGameMode();
        leaderboardEntry.setScore(rawScore);
        leaderboardEntry.setWeightedScore((int) Math.round(rawScore * mode.getScoreMultiplier()));
        leaderboardEntry.setDailyRun(dailyRun);
        leaderboardEntry.setGameMode(mode);
        leaderboardEntry.setCreatedAt(LocalDateTime.now());
        return leaderboardEntry;
    }
}
