package com.kore.king.entity;

public enum TransactionType {
    BET_CREATION,    // Points held when creating bet
    BET_ACCEPTANCE,  // Points held when accepting bet  
    WIN,             // Points awarded for winning
    REFUND,          // Points returned when bet cancelled
    DISPUTE_REFUND,  // Points returned due to dispute
    PLATFORM_FEE,    // Platform commission collected
    REFERRAL_BONUS   // Referral commission paid
}