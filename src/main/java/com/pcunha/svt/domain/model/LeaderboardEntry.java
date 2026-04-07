package com.pcunha.svt.domain.model;

import com.pcunha.svt.application.ScoreCalculator;
import com.pcunha.svt.domain.GameMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class LeaderboardEntry {
    @Id
    @GeneratedValue
    private Long id;
    private String playerName;
    private String teamName;
    private int turns;
    private boolean victory;
    private String lastLocation;
    private int health;
    private int energy;
    private int morale;
    private int cash;
    private int food;
    private int computeCredits;
    private int score;
    @Enumerated(EnumType.STRING)
    private GameMode gameMode;
    private LocalDateTime createdAt;

    public static LeaderboardEntry fromGameState(GameState gameState, String playerName) {
        LeaderboardEntry leaderboardEntry = new LeaderboardEntry();
        leaderboardEntry.playerName = playerName;
        leaderboardEntry.teamName = gameState.getTeamName();
        leaderboardEntry.turns = gameState.getProgressState().getTurn();
        leaderboardEntry.victory = gameState.getEndingState().isVictory();
        leaderboardEntry.lastLocation = gameState.getJourneyState().getCurrentLocation().getName();
        leaderboardEntry.health = gameState.getTeamState().getHealth();
        leaderboardEntry.energy = gameState.getTeamState().getEnergy();
        leaderboardEntry.morale = gameState.getTeamState().getMorale();
        leaderboardEntry.cash = gameState.getResourceState().getCash();
        leaderboardEntry.food = gameState.getResourceState().getFood();
        leaderboardEntry.computeCredits = gameState.getResourceState().getComputeCredits();
        leaderboardEntry.score = ScoreCalculator.calculate(gameState);
        leaderboardEntry.gameMode = gameState.getConfigState().getGameMode();
        leaderboardEntry.createdAt = LocalDateTime.now();
        return leaderboardEntry;
    }
}
