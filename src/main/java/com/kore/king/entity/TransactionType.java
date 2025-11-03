package com.kore.king.entity;

public enum TransactionType {
    CREATION,    // Points deducted when creating bet
    WIN,         // Points awarded for winning
    REFUND,      // Points returned when bet cancelled
    MANUAL_ADJUSTMENT // Admin manual adjustment
}
