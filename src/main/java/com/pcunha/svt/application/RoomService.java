package com.pcunha.svt.application;

import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.RoomStatus;
import com.pcunha.svt.domain.RoomType;
import com.pcunha.svt.domain.model.GameSession;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.Room;
import com.pcunha.svt.domain.port.GameSessionPort;
import com.pcunha.svt.domain.port.RoomPort;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns Room and GameSession lifecycle and GameState deserialization.
 */
public class RoomService {
    private final RoomPort roomPort;
    private final GameSessionPort gameSessionPort;
    private final GameEngine gameEngine;

    public RoomService(RoomPort roomPort, GameSessionPort gameSessionPort, GameEngine gameEngine) {
        this.roomPort = roomPort;
        this.gameSessionPort = gameSessionPort;
        this.gameEngine = gameEngine;
    }

    public LoadedSession createSoloGame(String playerToken, String teamName, GameMode mode) {
        Room room = new Room();
        room.setId(UUID.randomUUID().toString());
        room.setType(RoomType.SOLO);
        room.setMode(mode);
        room.setSeed(System.nanoTime());
        room.setStatus(RoomStatus.ACTIVE);
        room.setCreatedAt(LocalDateTime.now());
        roomPort.save(room);

        GameState gameState = gameEngine.createNewGame(teamName, mode);
        GameSession session = newSession(room.getId(), playerToken, teamName, gameState);
        return new LoadedSession(session, gameState);
    }

    public LoadedSession createOrJoinDailyGame(String playerToken, String playerName, GameMode mode) {
        LocalDate today = LocalDate.now();
        Room room = roomPort.findDailyRoom(mode, today).orElseGet(() -> {
            Room fresh = new Room();
            fresh.setId(UUID.randomUUID().toString());
            fresh.setType(RoomType.DAILY);
            fresh.setMode(mode);
            fresh.setSeed(seedFor(mode, today));
            fresh.setStatus(RoomStatus.ACTIVE);
            fresh.setCreatedAt(LocalDateTime.now());
            fresh.setDateBucket(today);
            return roomPort.save(fresh);
        });

        Optional<GameSession> existing = gameSessionPort.findByRoomAndPlayer(room.getId(), playerToken);
        if (existing.isPresent()) {
            return new LoadedSession(existing.get(), deserialize(existing.get().getGameStateData()));
        }

        GameState fresh = gameEngine.createNewGame(playerName, mode);
        GameSession session = newSession(room.getId(), playerToken, playerName, fresh);
        return new LoadedSession(session, fresh);
    }

    public boolean isDailySession(LoadedSession loaded) {
        return roomPort.findById(loaded.session().getRoomId())
                .map(r -> r.getType() == RoomType.DAILY)
                .orElse(false);
    }

    public Optional<LoadedSession> loadActiveSession(String playerToken) {
        return gameSessionPort.findActiveByPlayerToken(playerToken)
                .map(session -> new LoadedSession(session, deserialize(session.getGameStateData())));
    }

    public GameSession persist(LoadedSession loaded) {
        GameSession session = loaded.session();
        session.setGameStateData(serialize(loaded.gameState()));
        session.setLastActionAt(LocalDateTime.now());
        return gameSessionPort.save(session);
    }

    public void markCompleted(LoadedSession loaded) {
        GameSession session = loaded.session();
        session.setGameStateData(serialize(loaded.gameState()));
        session.setCompleted(true);
        session.setLastActionAt(LocalDateTime.now());
        gameSessionPort.save(session);
    }

    private GameSession newSession(String roomId, String playerToken, String playerName, GameState gameState) {
        GameSession session = new GameSession();
        session.setId(UUID.randomUUID().toString());
        session.setRoomId(roomId);
        session.setPlayerToken(playerToken);
        session.setPlayerName(playerName);
        session.setGameStateData(serialize(gameState));
        session.setCreatedAt(LocalDateTime.now());
        session.setLastActionAt(LocalDateTime.now());
        return gameSessionPort.save(session);
    }

    private static long seedFor(GameMode mode, LocalDate date) {
        return ((long) date.toString().hashCode() << 32) ^ mode.name().hashCode();
    }

    private static String serialize(GameState state) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(state);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize GameState", e);
        }
    }

    private static GameState deserialize(String data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalStateException("GameSession has no stored state");
        }
        byte[] bytes = Base64.getDecoder().decode(data);
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (GameState) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialize GameState", e);
        }
    }

    public record LoadedSession(GameSession session, GameState gameState) {
    }
}
