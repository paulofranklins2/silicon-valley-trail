package com.pcunha.svt.application;

import com.pcunha.svt.domain.model.*;
import com.pcunha.svt.domain.port.LeaderboardPort;
import com.pcunha.svt.infrastructure.data.GameDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LeaderboardServiceTest {

    private StubLeaderboardPort port;
    private LeaderboardService service;
    private GameState gameState;

    @BeforeEach
    void setUp() {
        port = new StubLeaderboardPort();
        ScoreCalculator scoreCalculator = new ScoreCalculator(GameDataLoader.loadScoring());
        service = new LeaderboardService(port, scoreCalculator);
        gameState = new GameState(
                new TeamState(100, 100, 100),
                new ResourceState(100, 10, 5),
                new JourneyState(
                        List.of(new Location("San Jose", 37.3, -121.8), new Location("San Francisco", 37.8, -122.4)),
                        List.of(80.0)
                ),
                "TestTeam"
        );
    }

    @Test
    void submitWithValidNameSucceedsAndMarksSubmitted() {
        SubmissionResult result = service.submitResult(gameState, "Alice", false);

        assertTrue(result.ok());
        assertEquals(1, port.saved.size());
        assertEquals("Alice", port.saved.getFirst().getPlayerName());
        assertTrue(gameState.getEndingState().isLeaderboardSubmitted());
    }

    @Test
    void submitTrimsWhitespaceFromPlayerName() {
        SubmissionResult result = service.submitResult(gameState, "  Bob  ", false);

        assertTrue(result.ok());
        assertEquals("Bob", port.saved.getFirst().getPlayerName());
    }

    @Test
    void submitWithNullNameReturnsErrorAndDoesNotCallPort() {
        SubmissionResult result = service.submitResult(gameState, null, false);

        assertFalse(result.ok());
        assertEquals("Name required", result.error());
        assertEquals(0, port.saved.size());
        assertFalse(gameState.getEndingState().isLeaderboardSubmitted());
    }

    @Test
    void submitWithBlankNameReturnsErrorAndDoesNotCallPort() {
        SubmissionResult result = service.submitResult(gameState, "   ", false);

        assertFalse(result.ok());
        assertEquals("Name required", result.error());
        assertEquals(0, port.saved.size());
        assertFalse(gameState.getEndingState().isLeaderboardSubmitted());
    }

    @Test
    void submitWithNameLongerThanMaxReturnsErrorAndDoesNotCallPort() {
        // Max is 10 chars; "ThisNameIsTooLong" is 17 chars.
        SubmissionResult result = service.submitResult(gameState, "ThisNameIsTooLong", false);

        assertFalse(result.ok());
        assertTrue(result.error().startsWith("Name too long"));
        assertEquals(0, port.saved.size());
        assertFalse(gameState.getEndingState().isLeaderboardSubmitted());
    }

    @Test
    void submitWithNameExactlyMaxLengthSucceeds() {
        // 10 characters, the boundary case
        SubmissionResult result = service.submitResult(gameState, "TenCharsOk", false);

        assertTrue(result.ok());
        assertEquals("TenCharsOk", port.saved.getFirst().getPlayerName());
    }

    @Test
    void submitWhenAlreadySubmittedReturnsErrorAndDoesNotCallPortAgain() {
        service.submitResult(gameState, "Alice", false);
        port.saved.clear();

        SubmissionResult result = service.submitResult(gameState, "Alice", false);

        assertFalse(result.ok());
        assertEquals("Already submitted", result.error());
        assertEquals(0, port.saved.size());
    }

    @Test
    void getTopScoresReturnsListFromPort() {
        var scores = service.getTopScores();

        assertNotNull(scores);
        assertTrue(scores.isEmpty());
    }

    @Test
    void submittedEntryHasWeightedScoreEqualToRawTimesMultiplier() {
        service.submitResult(gameState, "Alice", false);

        LeaderboardEntry entry = port.saved.getFirst();
        double multiplier = entry.getGameMode().getScoreMultiplier();
        assertEquals((int) Math.round(entry.getScore() * multiplier), entry.getWeightedScore());
    }

    private static class StubLeaderboardPort implements LeaderboardPort {
        final List<LeaderboardEntry> saved = new ArrayList<>();

        @Override
        public void save(LeaderboardEntry leaderboardEntry) {
            saved.add(leaderboardEntry);
        }

        @Override
        public List<LeaderboardEntry> getTopScores() {
            return List.of();
        }

        @Override
        public List<LeaderboardEntry> getDailyTopScores(java.time.LocalDateTime start, java.time.LocalDateTime end) {
            return List.of();
        }
    }
}
