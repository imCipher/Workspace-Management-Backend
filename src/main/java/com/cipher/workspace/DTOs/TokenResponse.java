package com.cipher.workspace.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token response containing access token and token type")
public record TokenResponse(
        @Schema(description = "JWT Access token")
        String accessToken,

        @Schema(description = "Token type", example = "Bearer")
        String tokenType
) {
    public TokenResponse(String accessToken) {
        this(accessToken, "Bearer");
    }
}
