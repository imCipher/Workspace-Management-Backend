package com.cipher.workspace.Entity;

public enum VisitStatus {
    ACTIVE("Active"),
    COMPLETED("Completed");

    private final String displayName;

    VisitStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
