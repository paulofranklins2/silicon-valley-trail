package com.pcunha.svt.application;

import com.pcunha.svt.domain.EventCategory;
import com.pcunha.svt.domain.WeatherCategory;
import com.pcunha.svt.domain.model.EventOutcome;
import com.pcunha.svt.domain.model.GameEvent;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.WeatherSignal;
import com.pcunha.svt.infrastructure.data.GameDataLoader;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class EventProcessor {

    // Chance to pick a weather-based event.
    // Higher on rough weather, lower on clear days.
    private static final double WEATHER_BIAS_ROUGH = 0.7;
    private static final double WEATHER_BIAS_CLEAR = 0.15;

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

    /**
     * Picks an event when arriving at a city.
     * Weather can influence the type of event picked.
     */
    public GameEvent generateEvent(WeatherSignal weatherSignal) {
        double bias = weatherBias(weatherSignal);
        List<GameEvent> weatherEvents = eventPool.get(EventCategory.WEATHER);

        if (weatherEvents != null && !weatherEvents.isEmpty() && random.nextDouble() < bias) {
            return weatherEvents.get(random.nextInt(weatherEvents.size()));
        }

        List<GameEvent> allEvents = eventPool.values().stream()
                .flatMap(List::stream)
                .toList();
        return allEvents.get(random.nextInt(allEvents.size()));
    }

    private double weatherBias(WeatherSignal signal) {
        if (signal == null) return 0.0;
        return signal.weatherCategory() == WeatherCategory.CLEAR ? WEATHER_BIAS_CLEAR : WEATHER_BIAS_ROUGH;
    }

    public GameEvent getCityMarketEvent() {
        return cityMarketEvents.get(random.nextInt(cityMarketEvents.size()));
    }
}
