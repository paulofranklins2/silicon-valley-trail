package com.pcunha.svt.application;

import com.pcunha.svt.domain.EventCategory;
import com.pcunha.svt.domain.model.EventOutcome;
import com.pcunha.svt.domain.model.GameEvent;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.WeatherSignal;
import com.pcunha.svt.infrastructure.data.GameDataLoader;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class EventProcessor {
    private final Random random;
    private final Map<EventCategory, List<GameEvent>> eventPool;
    private final List<GameEvent> cityMarketEvents;

    public EventProcessor(Random random, Map<EventCategory, List<GameEvent>> eventPool, List<GameEvent> cityMarketEvents) {
        this.random = random;
        this.eventPool = eventPool;
        this.cityMarketEvents = cityMarketEvents;
    }

    public static EventProcessor create(Random random) {
        return new EventProcessor(random, GameDataLoader.loadEvents(), GameDataLoader.loadMarkets());
    }

    public void applyEvent(GameState state, GameEvent event) {
        applyChanges(state, event.getHealthChange(), event.getEnergyChange(), event.getMoraleChange(),
                event.getCashChange(), event.getFoodChange(), event.getComputeCreditsChange());
    }

    public void applyOutcome(GameState state, EventOutcome outcome) {
        applyChanges(state, outcome.getHealthChange(), outcome.getEnergyChange(), outcome.getMoraleChange(),
                outcome.getCashChange(), outcome.getFoodChange(), outcome.getComputeCreditsChange());
    }

    private void applyChanges(GameState state, int health, int energy, int morale, int cash, int food, int compute) {
        state.getTeamState().changeHealth(health);
        state.getTeamState().changeEnergy(energy);
        state.getTeamState().changeMorale(morale);
        state.getResourceState().changeCash(cash);
        state.getResourceState().changeFood(food);
        state.getResourceState().changeComputeCredits(compute);
    }

    public GameEvent generateEvent(GameState gameState, WeatherSignal weatherSignal) {
        List<GameEvent> allEvents = eventPool.values().stream()
                .flatMap(List::stream)
                .toList();
        return allEvents.get(random.nextInt(allEvents.size()));
    }

    public GameEvent getCityMarketEvent() {
        return cityMarketEvents.get(random.nextInt(cityMarketEvents.size()));
    }
}
