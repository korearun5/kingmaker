package com.kore.king.entity;

public enum TicketCategory {
    BUY_POINTS("Buy Points Issue"),
    WIN_AMOUNT("Win Amount Not Added"),
    GAME_CANCELLED("Game Cancelled - Points Not Returned"),
    WITHDRAWAL("Withdrawal Issue"),
    TECHNICAL("Technical Problem"),
    DISPUTE("Game Dispute"),
    OTHER("Other");

    private final String displayName;

    TicketCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}