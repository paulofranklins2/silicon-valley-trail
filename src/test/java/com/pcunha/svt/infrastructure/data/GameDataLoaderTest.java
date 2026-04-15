package com.pcunha.svt.infrastructure.data;

import com.pcunha.svt.domain.EventCategory;
import com.pcunha.svt.domain.model.GameEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GameDataLoaderTest {

    @Test
    public void loadEventReturnsAllCategories() {
        // call loader
        Map<EventCategory, List<GameEvent>> eventCategoryListMap = GameDataLoader.loadEvents();

        // verify all 6 categories are present
        assertEquals(6, eventCategoryListMap.size());
        assertTrue(eventCategoryListMap.containsKey(EventCategory.LOCATION));
        assertTrue(eventCategoryListMap.containsKey(EventCategory.WEATHER));
        assertTrue(eventCategoryListMap.containsKey(EventCategory.TECH));
        assertTrue(eventCategoryListMap.containsKey(EventCategory.TEAM));
        assertTrue(eventCategoryListMap.containsKey(EventCategory.MARKET));
        assertTrue(eventCategoryListMap.containsKey(EventCategory.BOSS));

        // verify each category has event
        eventCategoryListMap.forEach((category, events) -> {
            assertFalse(events.isEmpty(), category + " should have events");
        });

        // spot check
        GameEvent gameEvent = eventCategoryListMap.get(EventCategory.WEATHER).stream()
                .filter(e -> e.getTitle().equals("Heat Wave"))
                .findFirst()
                .orElseThrow();

        assertEquals(-10, gameEvent.getEnergyChange());
        assertEquals(-5, gameEvent.getMoraleChange());

        GameEvent bossEvent = eventCategoryListMap.get(EventCategory.BOSS).stream()
                .filter(e -> e.getTitle().equals("Enterprise Procurement Gauntlet"))
                .findFirst()
                .orElseThrow();
        assertEquals(5, bossEvent.getCityIndex());
    }

    @Test
    void loadLocationsReturnsAllCities() {
        var locations = GameDataLoader.loadLocations();
        assertFalse(locations.isEmpty());
        assertEquals("San Jose", locations.getFirst().getName());
        assertEquals("San Francisco", locations.getLast().getName());
    }

    @Test
    void loadMarketsReturnsAllVariants() {
        var markets = GameDataLoader.loadMarkets();
        assertFalse(markets.isEmpty());
        // every market should have outcomes (purchase options)
        markets.forEach(m -> assertNotNull(m.getOutcomes()));
    }

}
