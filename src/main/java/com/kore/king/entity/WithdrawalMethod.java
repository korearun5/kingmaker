package com.kore.king.entity;

public enum WithdrawalMethod {
    MANUAL_UPI("Manual UPI"),
    MANUAL_BANK("Manual Bank Transfer"),
    RAZORPAY("Razorpay");

    private final String displayName;

    WithdrawalMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}