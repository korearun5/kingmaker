package com.kore.king.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kore.king.entity.User;

@Service
public class BetValidationService {

    @Value("${app.betting.min-points:10}")
    private Integer minPoints;

    @Value("${app.betting.max-points:1000}")
    private Integer maxPoints;

    @Value("${app.betting.max-active-bets:5}")
    private Integer maxActiveBets;

    public void validateBetCreation(User user, Integer points, String gameType) {
        // Check minimum points
        if (points < minPoints) {
            throw new RuntimeException("Minimum bet is " + minPoints + " points");
        }

        // Check maximum points
        if (points > maxPoints) {
            throw new RuntimeException("Maximum bet is " + maxPoints + " points");
        }

        // Check user has enough points
        if (!user.canAffordBet(points)) {
            throw new RuntimeException("Insufficient points. Available: " + user.getAvailablePoints());
        }

        // Validate game type
        if (gameType == null || gameType.trim().isEmpty()) {
            throw new RuntimeException("Game type is required");
        }

        if (gameType.length() > 50) {
            throw new RuntimeException("Game type too long");
        }
    }

    public void validateUserCanBet(User user) {
        if (user.getAvailablePoints() < minPoints) {
            throw new RuntimeException("You need at least " + minPoints + " points to create a bet");
        }
    }
}