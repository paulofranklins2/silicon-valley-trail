package com.pcunha.svt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Value;

@SpringBootApplication
public class SiliconValleyTrailApplication {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    public static void main(String[] args) {
        SpringApplication.run(SiliconValleyTrailApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String dbType = dbUrl.contains("postgresql") ? "PostgreSQL" : "H2 (in-memory)";
        System.out.println("\n  Silicon Valley Trail");
        System.out.println("  Database: " + dbType);
        System.out.println("  http://localhost:8080\n");
    }
}
