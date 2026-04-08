package com.pcunha.svt.infrastructure.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.pcunha.svt.application.Scoring;
import com.pcunha.svt.application.Tunables;
import com.pcunha.svt.domain.EventCategory;
import com.pcunha.svt.domain.GameAction;
import com.pcunha.svt.domain.model.ActionInfo;
import com.pcunha.svt.domain.model.GameEvent;
import com.pcunha.svt.domain.model.Location;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GameDataLoader {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final String PATH_ACTIONS = "data/actions.yml";
    private static final String PATH_LOCATIONS = "data/locations.yml";
    private static final String PATH_MARKETS = "data/markets.yml";
    private static final String PATH_EVENTS = "data/events.yml";
    private static final String PATH_TUNABLES = "data/tunables.yml";
    private static final String PATH_SCORING = "data/scoring.yml";

    public static List<ActionInfo> loadActions() {
        return load(PATH_ACTIONS, new TypeReference<>() {
        });
    }

    public static Map<GameAction, ActionInfo> loadActionMap() {
        List<ActionInfo> actions = loadActions();
        Map<GameAction, ActionInfo> map = new java.util.EnumMap<>(GameAction.class);
        actions.forEach(a -> map.put(a.getAction(), a));
        return map;
    }

    public static List<Location> loadLocations() {
        return load(PATH_LOCATIONS, new TypeReference<>() {
        });
    }

    public static List<GameEvent> loadMarkets() {
        List<GameEvent> markets = load(PATH_MARKETS, new TypeReference<>() {
        });
        markets.forEach(m -> m.setEventCategory(EventCategory.LOCATION));
        return markets;
    }

    public static Tunables loadTunables() {
        return load(PATH_TUNABLES, new TypeReference<>() {
        });
    }

    public static Scoring loadScoring() {
        return load(PATH_SCORING, new TypeReference<>() {
        });
    }

    public static Map<EventCategory, List<GameEvent>> loadEvents() {
        Map<String, List<GameEvent>> raw = load(PATH_EVENTS, new TypeReference<>() {
        });
        Map<EventCategory, List<GameEvent>> pool = new EnumMap<>(EventCategory.class);

        raw.forEach((key, events) -> {
            EventCategory category = EventCategory.valueOf(key.toUpperCase());
            events.forEach(e -> e.setEventCategory(category));
            pool.put(category, events);
        });

        return pool;
    }

    private static <T> T load(String path, TypeReference<T> type) {
        try (InputStream is = GameDataLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IllegalStateException("Missing data file: " + path);
            return YAML.readValue(is, type);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + path, e);
        }
    }
}
