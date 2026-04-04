package com.pcunha.svt.infrastructure.web.config;

import com.pcunha.svt.application.ActionHandler;
import com.pcunha.svt.application.ConditionEvaluator;
import com.pcunha.svt.application.EventProcessor;
import com.pcunha.svt.application.GameEngine;
import com.pcunha.svt.application.TurnProcessor;
import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.port.DistancePort;
import com.pcunha.svt.domain.port.WeatherPort;
import com.pcunha.svt.infrastructure.api.DemoWeatherAdapter;
import com.pcunha.svt.infrastructure.api.HaversineDistanceAdapter;
import com.pcunha.svt.infrastructure.api.MockWeatherAdapter;
import com.pcunha.svt.infrastructure.api.OpenMeteoAdapter;
import com.pcunha.svt.infrastructure.api.OsrmDistanceAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Random;

@Configuration
public class GameConfig {

    @Value("${game.weather.mode:api}")
    private String weatherMode;

    @Bean
    public Random random() {
        return new Random();
    }

    @Bean
    public WeatherPort weatherPort(Random random) {
        return switch (weatherMode) {
            case "mock" -> new MockWeatherAdapter(random);
            case "demo" -> new DemoWeatherAdapter();
            default -> new OpenMeteoAdapter(new MockWeatherAdapter(random));
        };
    }

    @Bean
    public ActionHandler actionHandler(Random random) {
        return ActionHandler.create(random);
    }

    @Bean
    public ConditionEvaluator conditionEvaluator() {
        return new ConditionEvaluator();
    }

    @Bean
    public EventProcessor eventProcessor(Random random) {
        return EventProcessor.create(random);
    }

    @Bean
    public TurnProcessor turnProcessor(ActionHandler actionHandler, ConditionEvaluator conditionEvaluator, EventProcessor eventProcessor, Random random, WeatherPort weatherPort) {
        return new TurnProcessor(actionHandler, conditionEvaluator, eventProcessor, random, weatherPort);
    }

    @Bean
    public GameEngine gameEngine(TurnProcessor turnProcessor) {
        HaversineDistanceAdapter haversine = new HaversineDistanceAdapter();
        OsrmDistanceAdapter osrm = new OsrmDistanceAdapter(haversine);

        Map<GameMode, DistancePort> distancePorts = Map.of(
                GameMode.FAST, haversine,
                GameMode.ROAD, osrm
        );

        return new GameEngine(turnProcessor, distancePorts);
    }
}
