package com.cipher.workspace.DTOs;

import com.cipher.workspace.Entity.ExpenseCategoryEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Represents an expense category record")
public record ExpenseCategoryResponse(
        @Schema(description = "Category ID")
        UUID id,

        @Schema(description = "Unique category name/identifier", example = "UTILITIES")
        String name,

        @Schema(description = "Human-readable display name", example = "Utilities")
        String displayName,

        @Schema(description = "Whether this is a system base category", example = "true")
        boolean isDefault,

        @Schema(description = "Creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Last update timestamp")
        LocalDateTime updatedAt
) {
    public static ExpenseCategoryResponse from(ExpenseCategoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ExpenseCategoryResponse(
                entity.getId(),
                entity.getName(),
                entity.getDisplayName(),
                entity.isDefault(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
