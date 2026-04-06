package com.pcunha.svt.infrastructure.web.config;

import com.pcunha.svt.application.*;
import com.pcunha.svt.domain.GameMode;
import com.pcunha.svt.domain.port.DistancePort;
import com.pcunha.svt.domain.port.WeatherPort;
import com.pcunha.svt.infrastructure.api.*;
import com.pcunha.svt.infrastructure.data.GameDataLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

@Configuration
public class GameConfig {

    @Value("${game.weather.mode:api}")
    private String weatherMode;

    @Value("${ORS_API_KEY:}")
    private String orsApiKey;

    @Bean
    public Random random() {
        return new Random();
    }

    @Bean
    public WeatherPort weatherPort(Random random) {
        return switch (weatherMode) {
            case "mock" -> new MockWeatherAdapter(random);
            case "demo" -> new DemoWeatherAdapter();
            default -> {
                OpenMeteoAdapter adapter = new OpenMeteoAdapter(new MockWeatherAdapter(random));
                adapter.preloadWeather(GameDataLoader.loadLocations());
                yield adapter;
            }
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
    public TurnProcessor turnProcessor(ActionHandler actionHandler, ConditionEvaluator conditionEvaluator,
                                       EventProcessor eventProcessor, Random random, WeatherPort weatherPort) {
        return new TurnProcessor(actionHandler, conditionEvaluator, eventProcessor, random, weatherPort);
    }

    @Bean
    public GameEngine gameEngine(TurnProcessor turnProcessor) {
        HaversineDistanceAdapter haversine = new HaversineDistanceAdapter();
        OpenRouteServiceAdapter ors = new OpenRouteServiceAdapter(haversine, orsApiKey);
        OsrmDistanceAdapter osrm = new OsrmDistanceAdapter(ors);

        Map<GameMode, DistancePort> distancePorts = new EnumMap<>(GameMode.class);
        distancePorts.put(GameMode.FAST, haversine);
        distancePorts.put(GameMode.ROAD, osrm);
        distancePorts.put(GameMode.WALKING_ROAD, osrm);
        distancePorts.put(GameMode.WALKING_FAST, haversine);

        return new GameEngine(turnProcessor, distancePorts);
    }
}
