package com.kore.king.entity;

public enum BetStatus {
    PENDING,        // Bet created, waiting for acceptance
    ACCEPTED,       // Bet accepted, waiting for game code  
    CODE_SHARED,    // Game code shared, waiting for results
    RESULTS_SUBMITTED, // Results submitted, waiting for verification
    COMPLETED,      // Bet completed and points transferred
    CANCELLED,      // Bet cancelled
    DISPUTED        // Results disputed
}