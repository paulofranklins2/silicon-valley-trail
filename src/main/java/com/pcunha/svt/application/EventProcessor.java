package com.pcunha.svt.application;

import com.pcunha.svt.domain.EventCategory;
import com.pcunha.svt.domain.model.EventOutcome;
import com.pcunha.svt.domain.model.GameEvent;
import com.pcunha.svt.domain.model.GameState;
import com.pcunha.svt.domain.model.WeatherSignal;

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
                        .energyChange(-5).foodChange(-1).build(),
                // choice: take shelter or push through
                GameEvent.builder()
                        .title("Incoming Storm")
                        .description("Dark clouds are rolling in. Do you take shelter or push through?")
                        .eventCategory(EventCategory.WEATHER)
                        .outcomes(List.of(
                                EventOutcome.builder()
                                        .description("Take shelter and wait it out")
                                        .foodChange(-2).energyChange(10).build(),
                                EventOutcome.builder()
                                        .description("Push through the storm")
                                        .healthChange(-10).energyChange(-15).moraleChange(5).build()
                        )).build()
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
                        .healthChange(-10).energyChange(-10).build(),
                // choice: team member wants to leave
                GameEvent.builder()
                        .title("Engineer Wants to Quit")
                        .description("Your lead engineer got a better offer. Convince them to stay or let them go?")
                        .eventCategory(EventCategory.TEAM)
                        .outcomes(List.of(
                                EventOutcome.builder()
                                        .description("Offer a raise to keep them")
                                        .cashChange(-30).computeCreditsChange(5).moraleChange(5).build(),
                                EventOutcome.builder()
                                        .description("Let them go and redistribute work")
                                        .moraleChange(-10).energyChange(-10).build()
                        )).build()
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
                // choice: VC equity deal
                GameEvent.builder()
                        .title("VC Equity Offer")
                        .description("A VC offers to fund your startup but wants 40% equity.")
                        .eventCategory(EventCategory.MARKET)
                        .outcomes(List.of(
                                EventOutcome.builder()
                                        .description("Accept the deal")
                                        .cashChange(200).moraleChange(-20).build(),
                                EventOutcome.builder()
                                        .description("Decline and stay independent")
                                        .moraleChange(10).build()
                        )).build()
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
                // choice: found abandoned supplies
                GameEvent.builder()
                        .title("Abandoned Office")
                        .description("You find an abandoned startup office. Scavenge for supplies or check their servers?")
                        .eventCategory(EventCategory.LOCATION)
                        .outcomes(List.of(
                                EventOutcome.builder()
                                        .description("Scavenge for food and supplies")
                                        .foodChange(4).energyChange(-5).build(),
                                EventOutcome.builder()
                                        .description("Check their servers for compute credits")
                                        .computeCreditsChange(8).energyChange(-5).build()
                        )).build()
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
                // choice: technical debt
                GameEvent.builder()
                        .title("Technical Debt Crisis")
                        .description("The codebase is falling apart. Spend time fixing it or ship fast and deal with it later?")
                        .eventCategory(EventCategory.TECH)
                        .outcomes(List.of(
                                EventOutcome.builder()
                                        .description("Fix the tech debt now")
                                        .computeCreditsChange(10).energyChange(-20).foodChange(-1).build(),
                                EventOutcome.builder()
                                        .description("Ship fast, deal with it later")
                                        .moraleChange(-10).computeCreditsChange(-5).build()
                        )).build()
        ));

        return pool;
    }
}
