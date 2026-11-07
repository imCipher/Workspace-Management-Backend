package com.cipher.workspace.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body for adding a new dynamic option (service type, payment type, or expense category)")
public class CreateOptionRequest {

    @Schema(description = "Identifier/code", example = "PODCAST_STUDIO")
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9_ -]+$", message = "Name must only contain alphanumeric characters, spaces, dashes, or underscores")
    private String name;

    @Schema(description = "Human-readable display name", example = "Podcast Studio")
    @NotBlank(message = "Display name is required")
    @Size(min = 2, max = 100, message = "Display name must be between 2 and 100 characters")
    private String displayName;
}
