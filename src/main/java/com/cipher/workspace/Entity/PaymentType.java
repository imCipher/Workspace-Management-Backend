package com.cipher.workspace.Entity;

import com.cipher.workspace.DTOs.EnumOptionResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.List;

public enum PaymentType {
    CASH("Cash"),
    CREDIT_CARD("Credit Card"),
    ONLINE_TRANSFER("Online Transfer"),
    CHECK("Check");

    private final String displayName;

    PaymentType(String displayName) {
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
                .map(PaymentType::toOption)
                .toList();
    }

    @JsonCreator
    public static PaymentType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        for (PaymentType type : values()) {
            if (type.name().equalsIgnoreCase(normalized) || type.displayName.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown payment type: " + value);
    }
}
