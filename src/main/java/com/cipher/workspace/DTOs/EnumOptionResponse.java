package com.cipher.workspace.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a selectable option with its identifier, display label, and default status")
public record EnumOptionResponse(
        @Schema(description = "Option value/code", example = "SHARED_WORKSPACE")
        String value,

        @Schema(description = "Human-readable display label", example = "Shared Workspace")
        String label,

        @Schema(description = "Whether this is a system default option", example = "true")
        boolean isDefault
) {
    public EnumOptionResponse(String value, String label) {
        this(value, label, true);
    }

    public static EnumOptionResponse of(String value, String label) {
        return new EnumOptionResponse(value, label, true);
    }

    public static EnumOptionResponse of(String value, String label, boolean isDefault) {
        return new EnumOptionResponse(value, label, isDefault);
    }
}
