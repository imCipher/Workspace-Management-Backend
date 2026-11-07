package com.cipher.workspace.Entity;

import com.cipher.workspace.DTOs.EnumOptionResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.List;

public enum ServiceType {
    MEETING_ROOM("Meeting Room"),
    DEDICATED_DESK("Dedicated Desk"),
    SHARED_WORKSPACE("Shared Workspace"),
    PRIVATE_OFFICE("Private Office");

    private final String displayName;

    ServiceType(String displayName) {
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
                .map(ServiceType::toOption)
                .toList();
    }

    @JsonCreator
    public static ServiceType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        for (ServiceType type : values()) {
            if (type.name().equalsIgnoreCase(normalized) || type.displayName.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown service type: " + value);
    }
}
