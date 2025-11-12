package com.kore.king.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;


@Component
public class BetMetrics {

    private final Counter betsCreatedCounter;
    private final Counter betsAcceptedCounter;
    private final Counter betsCompletedCounter;
    private final Counter betsCancelledCounter;
    private final Timer betCreationTimer;
    private final Timer betResolutionTimer;

    public BetMetrics(MeterRegistry registry) {
        this.betsCreatedCounter = Counter.builder("betking.bets.created")
                .description("Number of bets created")
                .register(registry);

        this.betsAcceptedCounter = Counter.builder("betking.bets.accepted")
                .description("Number of bets accepted")
                .register(registry);

        this.betsCompletedCounter = Counter.builder("betking.bets.completed")
                .description("Number of bets completed")
                .register(registry);

        this.betsCancelledCounter = Counter.builder("betking.bets.cancelled")
                .description("Number of bets cancelled")
                .register(registry);

        this.betCreationTimer = Timer.builder("betking.bets.creation.time")
                .description("Time taken to create a bet")
                .register(registry);

        this.betResolutionTimer = Timer.builder("betking.bets.resolution.time")
                .description("Time taken to resolve a bet")
                .register(registry);
    }

    public void incrementBetsCreated() {
        betsCreatedCounter.increment();
    }

    public void incrementBetsAccepted() {
        betsAcceptedCounter.increment();
    }

    public void incrementBetsCompleted() {
        betsCompletedCounter.increment();
    }

    public void incrementBetsCancelled() {
        betsCancelledCounter.increment();
    }

    public Timer getBetCreationTimer() {
        return betCreationTimer;
    }

    public Timer getBetResolutionTimer() {
        return betResolutionTimer;
    }
}