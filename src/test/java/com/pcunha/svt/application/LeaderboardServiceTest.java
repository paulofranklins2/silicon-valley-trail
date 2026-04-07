package com.pcunha.svt.application;

import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.JourneyState;
import com.pcunha.svt.domain.model.LeaderboardEntry;
import com.pcunha.svt.domain.model.Location;
import com.pcunha.svt.domain.model.ResourceState;
import com.pcunha.svt.domain.model.SubmissionResult;
import com.pcunha.svt.domain.model.TeamState;
import com.pcunha.svt.domain.port.LeaderboardPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardServiceTest {

    private StubLeaderboardPort port;
    private LeaderboardService service;
    private GameState gameState;

    @BeforeEach
    void setUp() {
        port = new StubLeaderboardPort();
        service = new LeaderboardService(port);
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
        SubmissionResult result = service.submitResult(gameState, "Alice");

        assertTrue(result.ok());
        assertEquals(1, port.saved.size());
        assertEquals("Alice", port.saved.getFirst().getPlayerName());
        assertTrue(gameState.isLeaderboardSubmitted());
    }

    @Test
    void submitTrimsWhitespaceFromPlayerName() {
        SubmissionResult result = service.submitResult(gameState, "  Bob  ");

        assertTrue(result.ok());
        assertEquals("Bob", port.saved.getFirst().getPlayerName());
    }

    @Test
    void submitWithNullNameReturnsErrorAndDoesNotCallPort() {
        SubmissionResult result = service.submitResult(gameState, null);

        assertFalse(result.ok());
        assertEquals("Name required", result.error());
        assertEquals(0, port.saved.size());
        assertFalse(gameState.isLeaderboardSubmitted());
    }

    @Test
    void submitWithBlankNameReturnsErrorAndDoesNotCallPort() {
        SubmissionResult result = service.submitResult(gameState, "   ");

        assertFalse(result.ok());
        assertEquals("Name required", result.error());
        assertEquals(0, port.saved.size());
        assertFalse(gameState.isLeaderboardSubmitted());
    }

    @Test
    void submitWhenAlreadySubmittedReturnsErrorAndDoesNotCallPortAgain() {
        service.submitResult(gameState, "Alice");
        port.saved.clear();

        SubmissionResult result = service.submitResult(gameState, "Alice");

        assertFalse(result.ok());
        assertEquals("Already submitted", result.error());
        assertEquals(0, port.saved.size());
    }

    @Test
    void getTopScoresByModeReturnsAllModes() {
        var scores = service.getTopScoresByMode();

        assertEquals(GameMode.values().length, scores.size());
        for (GameMode mode : GameMode.values()) {
            assertTrue(scores.containsKey(mode));
        }
    }

    private static class StubLeaderboardPort implements LeaderboardPort {
        final List<LeaderboardEntry> saved = new ArrayList<>();

        @Override
        public void save(LeaderboardEntry leaderboardEntry) {
            saved.add(leaderboardEntry);
        }

        @Override
        public List<LeaderboardEntry> getTopScores(GameMode gameMode) {
            return List.of();
        }
    }
}
