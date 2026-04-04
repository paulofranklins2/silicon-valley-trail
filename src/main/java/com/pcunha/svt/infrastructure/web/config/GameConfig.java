package com.pcunha.svt.infrastructure.web.config;

import com.pcunha.svt.application.*;
import com.pcunha.svt.domain.port.DistancePort;
import com.pcunha.svt.domain.port.WeatherPort;
import com.pcunha.svt.infrastructure.api.DemoWeatherAdapter;
import com.pcunha.svt.infrastructure.api.HaversineDistanceAdapter;
import com.pcunha.svt.infrastructure.api.MockWeatherAdapter;
import com.pcunha.svt.infrastructure.api.OpenMeteoAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        return new ActionHandler(random);
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
    public DistancePort distancePort() {
        return new HaversineDistanceAdapter();
    }

    @Bean
    public GameEngine gameEngine(TurnProcessor turnProcessor, DistancePort distancePort) {
        return new GameEngine(turnProcessor, distancePort);
    }
}