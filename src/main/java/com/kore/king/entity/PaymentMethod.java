package com.kore.king.entity;

public enum PaymentMethod {
    MANUAL_UPI("Manual UPI"),
    MANUAL_BANK("Manual Bank Transfer"), 
    RAZORPAY("Razorpay");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}