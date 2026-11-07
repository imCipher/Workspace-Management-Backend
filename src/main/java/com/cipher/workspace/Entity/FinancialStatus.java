package com.cipher.workspace.Entity;

public enum FinancialStatus {
    PROFIT("Profit"),
    LOSS("Loss"),
    BREAK_EVEN("Break Even");

    private final String displayName;

    FinancialStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
