package com.pcunha.svt.infrastructure.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.pcunha.svt.domain.EventCategory;
import com.pcunha.svt.domain.model.GameEvent;
import com.pcunha.svt.domain.model.Location;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GameDataLoader {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final String PATH_LOCATIONS = "data/locations.yaml";
    private static final String PATH_MARKETS = "data/markets.yaml";
    private static final String PATH_EVENTS = "data/events.yaml";

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
