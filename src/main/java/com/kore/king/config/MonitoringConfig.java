package com.kore.king.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MonitoringConfig {

    @Bean
    public HealthIndicator databaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        return () -> {
            try {
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                return Health.up()
                        .withDetail("database", "Connected")
                        .withDetail("timestamp", System.currentTimeMillis())
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("database", "Disconnected")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    @Bean
    public HealthIndicator applicationHealthIndicator() {
        return () -> Health.up()
                .withDetail("application", "BetKing")
                .withDetail("status", "Running")
                .withDetail("timestamp", System.currentTimeMillis())
                .build();
    }

    @Bean
    public InfoContributor applicationInfoContributor() {
        return builder -> {
            Map<String, Object> details = new HashMap<>();
            details.put("name", "BetKing");
            details.put("version", "1.0.0");
            details.put("description", "Online Betting Platform");
            details.put("environment", System.getProperty("spring.profiles.active", "default"));
            builder.withDetails(details);
        };
    }
}