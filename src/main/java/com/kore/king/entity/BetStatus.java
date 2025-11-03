package com.kore.king.entity;

public enum BetStatus {
    PENDING,    // Bet is created and waiting for a match
    MATCHED,    // Bet is matched with another bet
    IN_PROGRESS, // The game is in progress
    COMPLETED,  // The game is completed and result is submitted
    CANCELLED,   // Bet is cancelled
    DISPUTED
}
