package com.kore.king.entity;

public enum BetStatus {
    PENDING,        // Bet created, waiting for acceptor
    ACCEPTED,       // Bet accepted by opponent
    CODE_SHARED,    // Game code shared by creator
    RESULTS_SUBMITTED, // One or both results submitted
    COMPLETED,      // Bet resolved with winner
    CANCELLED,      // Bet cancelled
    DISPUTED        // Results conflict
}