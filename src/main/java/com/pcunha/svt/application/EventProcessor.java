package com.pcunha.svt.application;

import com.pcunha.svt.domain.EventCategory;
import com.pcunha.svt.domain.GameEvent;
import com.pcunha.svt.domain.GameState;
import com.pcunha.svt.domain.WeatherSignal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class EventProcessor {
    private final Random random;
    private final Map<EventCategory, List<GameEvent>> eventPool;

    public EventProcessor(Random random) {
        this.random = random;
        this.eventPool = buildEventPool();
    }

    public void applyEvent(GameState state, GameEvent event) {
        state.getTeamState().changeHealth(event.getHealthChange());
        state.getTeamState().changeEnergy(event.getEnergyChange());
        state.getTeamState().changeMorale(event.getMoraleChange());
        state.getResourceState().changeCash(event.getCashChange());
        state.getResourceState().changeFood(event.getFoodChange());
        state.getResourceState().changeComputeCredits(event.getComputeCreditsChange());
    }

    public GameEvent generateEvent(GameState gameState, WeatherSignal weatherSignal) {
        List<GameEvent> allEvents = eventPool.values().stream()
                .flatMap(List::stream)
                .toList();
        return allEvents.get(random.nextInt(allEvents.size()));
    }

    private Map<EventCategory, List<GameEvent>> buildEventPool() {
        Map<EventCategory, List<GameEvent>> pool = new HashMap<>();

        pool.put(EventCategory.WEATHER, List.of(
                GameEvent.builder()
                        .title("Heat Wave")
                        .description("Scorching heat slows the team down")
                        .eventCategory(EventCategory.WEATHER)
                        .energyChange(-10).moraleChange(-5).build(),
                GameEvent.builder()
                        .title("Clear Skies")
                        .description("Perfect weather lifts the team's spirits")
                        .eventCategory(EventCategory.WEATHER)
                        .moraleChange(5).energyChange(5).build(),
                GameEvent.builder()
                        .title("Fog Delay")
                        .description("Dense fog makes it hard to move forward")
                        .eventCategory(EventCategory.WEATHER)
                        .energyChange(-5).foodChange(-1).build()
        ));

        pool.put(EventCategory.TEAM, List.of(
                GameEvent.builder()
                        .title("Team Argument")
                        .description("A disagreement breaks out among the team")
                        .eventCategory(EventCategory.TEAM)
                        .moraleChange(-15).build(),
                GameEvent.builder()
                        .title("Team Breakthrough")
                        .description("The team solves a tough problem together")
                        .eventCategory(EventCategory.TEAM)
                        .moraleChange(10).energyChange(5).build(),
                GameEvent.builder()
                        .title("Sick Day")
                        .description("A team member falls ill")
                        .eventCategory(EventCategory.TEAM)
                        .healthChange(-10).energyChange(-10).build()
        ));

        pool.put(EventCategory.MARKET, List.of(
                GameEvent.builder()
                        .title("Funding Opportunity")
                        .description("An angel investor shows interest")
                        .eventCategory(EventCategory.MARKET)
                        .cashChange(30).build(),
                GameEvent.builder()
                        .title("Market Crash")
                        .description("The market takes a hit and funding dries up")
                        .eventCategory(EventCategory.MARKET)
                        .cashChange(-20).moraleChange(-5).build(),
                GameEvent.builder()
                        .title("Acquisition Offer")
                        .description("A big company wants to buy your startup")
                        .eventCategory(EventCategory.MARKET)
                        .cashChange(50).moraleChange(-10).build()
        ));

        pool.put(EventCategory.LOCATION, List.of(
                GameEvent.builder()
                        .title("Coffee Shop Discovery")
                        .description("The team finds a great local coffee spot")
                        .eventCategory(EventCategory.LOCATION)
                        .energyChange(10).foodChange(2).build(),
                GameEvent.builder()
                        .title("Startup Meetup")
                        .description("A local meetup connects the team with other founders")
                        .eventCategory(EventCategory.LOCATION)
                        .moraleChange(5).cashChange(10).build(),
                GameEvent.builder()
                        .title("Stanford Recruiting")
                        .description("A Stanford grad wants to join but expects a signing bonus")
                        .eventCategory(EventCategory.LOCATION)
                        .computeCreditsChange(5).cashChange(-10).build()
        ));

        pool.put(EventCategory.TECH, List.of(
                GameEvent.builder()
                        .title("Server Outage")
                        .description("Production servers go down overnight")
                        .eventCategory(EventCategory.TECH)
                        .computeCreditsChange(-5).moraleChange(-5).build(),
                GameEvent.builder()
                        .title("Open Source Contribution")
                        .description("The team's open source work gets noticed")
                        .eventCategory(EventCategory.TECH)
                        .computeCreditsChange(5).moraleChange(5).build(),
                GameEvent.builder()
                        .title("Bug Infestation")
                        .description("A wave of bugs hits the codebase")
                        .eventCategory(EventCategory.TECH)
                        .computeCreditsChange(-3).energyChange(-10).build()
        ));

        return pool;
    }
}
