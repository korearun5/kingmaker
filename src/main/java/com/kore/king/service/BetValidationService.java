package com.kore.king.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.kore.king.entity.User;
import com.kore.king.exception.InsufficientPointsException;
import com.kore.king.exception.InvalidBetStateException;
import com.kore.king.repository.BetRepository;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Service
@Validated
public class BetValidationService {

    @Value("${app.betting.min-points:10}")
    private Integer minPoints;

    @Value("${app.betting.max-points:1000}")
    private Integer maxPoints;

    @Value("${app.betting.max-active-bets:5}")
    private Integer maxActiveBets;

    private final BetRepository betRepository;

    public BetValidationService(BetRepository betRepository) {
        this.betRepository = betRepository;
    }

    public void validateBetCreation(@NotNull User user, @Min(10) @Max(10000) Integer points, 
                                   @NotBlank String gameType) {
        
        // Check minimum points
        if (points < minPoints) {
            throw new InsufficientPointsException(user.getAvailablePoints(), points);
        }

        // Check maximum points
        if (points > maxPoints) {
            throw new InvalidBetStateException("Maximum bet is " + maxPoints + " points");
        }

        // Check user has enough points
        if (!user.canAffordBet(points)) {
            throw new InsufficientPointsException(user.getAvailablePoints(), points);
        }

        // Check maximum active bets
        if (betRepository.hasActiveBets(user.getId())) {
            throw new InvalidBetStateException("You have reached the maximum number of active bets");
        }

        // Validate game type
        if (gameType == null || gameType.trim().isEmpty()) {
            throw new InvalidBetStateException("Game type is required");
        }

        if (gameType.length() > 50) {
            throw new InvalidBetStateException("Game type too long");
        }
    }

    public void validateUserCanBet(@NotNull User user) {
        if (user.getAvailablePoints() < minPoints) {
            throw new InsufficientPointsException(user.getAvailablePoints(), minPoints);
        }
    }
}