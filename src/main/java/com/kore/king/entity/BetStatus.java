package com.kore.king.entity;

public enum BetStatus {
    PENDING,        // Bet created, waiting for acceptance
    ACCEPTED,       // Bet accepted, waiting for room code  
    CODE_SHARED,    // Room code shared, waiting for results
    COMPLETED,      // Match completed and resolved
    CANCELLED,      // Bet cancelled
    DISPUTED        // Results disputed (both claimed win or other issues)
}