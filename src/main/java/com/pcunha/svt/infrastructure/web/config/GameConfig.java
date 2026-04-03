package com.pcunha.svt.infrastructure.web.config;

import com.pcunha.svt.application.*;
import com.pcunha.svt.domain.port.DistancePort;
import com.pcunha.svt.domain.port.WeatherPort;
import com.pcunha.svt.infrastructure.api.HaversineDistanceAdapter;
import com.pcunha.svt.infrastructure.api.MockWeatherAdapter;
import com.pcunha.svt.infrastructure.api.OpenMeteoAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class GameConfig {

    @Value("${game.weather.use-mock:false}")
    private boolean useMockWeather;

    @Bean
    public Random random() {
        return new Random();
    }

    @Bean
    public WeatherPort weatherPort(Random random) {
        MockWeatherAdapter mock = new MockWeatherAdapter(random);
        if (useMockWeather) {
            return mock;
        }
        return new OpenMeteoAdapter(mock);
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
        return new EventProcessor(random);
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