package com.pcunha.svt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class SiliconValleyTrailApplication {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    public static void main(String[] args) {
        SpringApplication.run(SiliconValleyTrailApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String dbType;
        if (dbUrl.contains("postgresql")) {
            dbType = "PostgreSQL";
        } else {
            dbType = "H2";
        }
        System.out.println("\n  Silicon Valley Trail");
        System.out.println("Database: " + dbType);
        System.out.println("http://localhost:8080\n");
    }
}
