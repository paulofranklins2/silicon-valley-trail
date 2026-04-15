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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        return createSoloGame(playerToken, null, teamName, mode);
    }

    public LoadedSession createSoloGame(String playerToken, String userId, String teamName, GameMode mode) {
        Room room = new Room();
        room.setId(UUID.randomUUID().toString());
        room.setType(RoomType.SOLO);
        room.setMode(mode);
        room.setSeed(System.nanoTime());
        room.setStatus(RoomStatus.ACTIVE);
        room.setCreatedAt(LocalDateTime.now());
        roomPort.save(room);

        GameState gameState = gameEngine.createNewGame(teamName, mode);
        GameSession session = newSession(room.getId(), playerToken, userId, teamName, gameState);
        return new LoadedSession(session, gameState);
    }

    public LoadedSession createOrJoinDailyGame(String playerToken, String playerName, GameMode mode) {
        return createOrJoinDailyGame(playerToken, null, playerName, mode);
    }

    public LoadedSession createOrJoinDailyGame(String playerToken, String userId, String playerName, GameMode mode) {
        LocalDate today = LocalDate.now();
        purgeExpiredDailyData(today);
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

        Optional<GameSession> existing = userId != null
                ? gameSessionPort.findByRoomAndUser(room.getId(), userId)
                : gameSessionPort.findByRoomAndPlayer(room.getId(), playerToken);
        if (existing.isPresent()) {
            try {
                GameState gameState = deserialize(existing.get().getGameStateData());
                repairMissingStartTime(existing.get(), gameState);
                return new LoadedSession(existing.get(), gameState);
            } catch (IllegalStateException e) {
                GameState fresh = gameEngine.createNewGame(playerName, mode, room.getSeed());
                GameSession repaired = existing.get();
                repaired.setPlayerName(playerName);
                repaired.setGameStateData(serialize(fresh));
                repaired.setCompleted(false);
                repaired.setLastActionAt(LocalDateTime.now());
                gameSessionPort.save(repaired);
                return new LoadedSession(repaired, fresh);
            }
        }

        // Pass the room seed so all Daily players get the same random sequence
        GameState fresh = gameEngine.createNewGame(playerName, mode, room.getSeed());
        GameSession session = newSession(room.getId(), playerToken, userId, playerName, fresh);
        return new LoadedSession(session, fresh);
    }

    public void purgeExpiredDailyData() {
        purgeExpiredDailyData(LocalDate.now());
    }

    public boolean isDailySession(LoadedSession loaded) {
        return roomPort.findById(loaded.session().getRoomId())
                .map(r -> r.getType() == RoomType.DAILY)
                .orElse(false);
    }

    public Optional<LoadedSession> loadActiveSession(String playerToken) {
        return loadActiveSession(playerToken, null);
    }

    public Optional<LoadedSession> loadActiveSession(String playerToken, String userId) {
        for (GameSession session : findActiveSessions(playerToken, userId)) {
            Optional<LoadedSession> loaded = loadSession(session);
            if (loaded.isPresent()) {
                return loaded;
            }
        }
        return Optional.empty();
    }

    public Optional<LoadedSession> loadActiveSessionForSlot(String playerToken, String userId, String slotKey) {
        return loadActiveSessionsBySlot(playerToken, userId).entrySet().stream()
                .filter(entry -> entry.getKey().equals(slotKey))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public Map<String, LoadedSession> loadActiveSessionsBySlot(String playerToken, String userId) {
        Map<String, LoadedSession> sessionsBySlot = new LinkedHashMap<>();
        for (GameSession session : findActiveSessions(playerToken, userId)) {
            Room room = roomPort.findById(session.getRoomId()).orElse(null);
            if (room == null) {
                continue;
            }
            String slotKey = slotKey(room);
            if (sessionsBySlot.containsKey(slotKey)) {
                continue;
            }
            loadSession(session).ifPresent(loaded -> sessionsBySlot.put(slotKey, loaded));
        }
        return sessionsBySlot;
    }

    public void attachGuestSessionToUser(String playerToken, String userId) {
        if (playerToken == null || playerToken.isBlank() || userId == null || userId.isBlank()) return;
        Map<String, LoadedSession> userSessionsBySlot = loadActiveSessionsBySlot(playerToken, userId);
        for (GameSession session : gameSessionPort.findActiveSessionsByPlayerToken(playerToken)) {
            Room room = roomPort.findById(session.getRoomId()).orElse(null);
            if (room == null) continue;
            if (userSessionsBySlot.containsKey(slotKey(room))) continue;
            session.setUserId(userId);
            session.setLastActionAt(LocalDateTime.now());
            gameSessionPort.save(session);
        }
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

    public void touch(LoadedSession loaded) {
        loaded.session().setLastActionAt(LocalDateTime.now());
        gameSessionPort.save(loaded.session());
    }

    private GameSession newSession(String roomId, String playerToken, String userId, String playerName, GameState gameState) {
        GameSession session = new GameSession();
        session.setId(UUID.randomUUID().toString());
        session.setRoomId(roomId);
        session.setPlayerToken(playerToken);
        session.setUserId(userId);
        session.setPlayerName(playerName);
        session.setGameStateData(serialize(gameState));
        session.setCreatedAt(LocalDateTime.now());
        session.setLastActionAt(LocalDateTime.now());
        return gameSessionPort.save(session);
    }

    private static long seedFor(GameMode mode, LocalDate date) {
        return ((long) date.toString().hashCode() << 32) ^ mode.name().hashCode();
    }

    private void purgeExpiredDailyData(LocalDate today) {
        List<Room> expiredRooms = roomPort.findRoomsByTypeBeforeDate(RoomType.DAILY, today);
        if (expiredRooms.isEmpty()) return;
        List<String> roomIds = expiredRooms.stream().map(Room::getId).toList();
        gameSessionPort.deleteByRoomIds(roomIds);
        roomPort.deleteByIds(roomIds);
    }

    private List<GameSession> findActiveSessions(String playerToken, String userId) {
        return userId != null
                ? gameSessionPort.findActiveSessionsByUserId(userId)
                : gameSessionPort.findActiveSessionsByPlayerToken(playerToken);
    }

    private Optional<LoadedSession> loadSession(GameSession session) {
        try {
            GameState gameState = deserialize(session.getGameStateData());
            repairMissingStartTime(session, gameState);
            return Optional.of(new LoadedSession(session, gameState));
        } catch (IllegalStateException e) {
            session.setCompleted(true);
            session.setLastActionAt(LocalDateTime.now());
            gameSessionPort.save(session);
            return Optional.empty();
        }
    }

    private static String slotKey(Room room) {
        return room.getType() == RoomType.DAILY ? "DAILY" : room.getMode().name();
    }

    private void repairMissingStartTime(GameSession session, GameState gameState) {
        if (gameState.getProgressState().getStartTimeMs() != 0) return;
        gameState.getProgressState().setStartTimeMs(System.currentTimeMillis());
        session.setGameStateData(serialize(gameState));
        session.setLastActionAt(LocalDateTime.now());
        gameSessionPort.save(session);
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
