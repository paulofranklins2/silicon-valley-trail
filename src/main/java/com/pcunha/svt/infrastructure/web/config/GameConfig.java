package com.pcunha.svt.infrastructure.web.config;

import com.pcunha.svt.application.*;
import com.pcunha.svt.domain.port.DistancePort;
import com.pcunha.svt.infrastructure.api.HaversineDistanceAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class GameConfig {

    @Bean
    public Random random() {
        return new Random();
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
    public TurnProcessor turnProcessor(ActionHandler actionHandler, ConditionEvaluator conditionEvaluator, EventProcessor eventProcessor, Random random) {
        return new TurnProcessor(actionHandler, conditionEvaluator, eventProcessor, random);
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