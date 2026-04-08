package com.pcunha.svt.domain.model;

import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.LossReason;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Persistent record of a finished game.
 */
@Entity
@Getter
@Setter
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
    // Needed to recompute journey progress without resolving location names
    private int locationIndex;
    private int totalLocations;
    @Enumerated(EnumType.STRING)
    private LossReason lossReason;
    private int health;
    private int energy;
    private int morale;
    private int cash;
    private int food;
    private int computeCredits;
    private int score;
    private int weightedScore;
    private boolean dailyRun;
    @Enumerated(EnumType.STRING)
    private GameMode gameMode;
    private LocalDateTime createdAt;

    /**
     * Converts this entry into ScoreInputs for score recalculation.
     */
    public ScoreInputs toScoreInputs() {
        return new ScoreInputs(
                victory,
                turns,
                locationIndex,
                totalLocations,
                health,
                energy,
                morale,
                cash,
                food,
                computeCredits
        );
    }
}
