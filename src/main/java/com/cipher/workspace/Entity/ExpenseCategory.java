package com.cipher.workspace.Entity;

import com.cipher.workspace.DTOs.EnumOptionResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.List;

public enum ExpenseCategory {
    UTILITIES("Utilities"),
    MAINTENANCE("Maintenance"),
    PAYROLL("Payroll"),
    SUPPLIES("Supplies");

    private final String displayName;

    ExpenseCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonValue
    public String getValue() {
        return name();
    }

    public EnumOptionResponse toOption() {
        return new EnumOptionResponse(name(), displayName, true);
    }

    public static List<EnumOptionResponse> getAllOptions() {
        return Arrays.stream(values())
                .map(ExpenseCategory::toOption)
                .toList();
    }

    @JsonCreator
    public static ExpenseCategory fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        for (ExpenseCategory category : values()) {
            if (category.name().equalsIgnoreCase(normalized) || category.displayName.equalsIgnoreCase(value.trim())) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown expense category: " + value);
    }
}
