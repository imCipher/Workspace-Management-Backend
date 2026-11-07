package com.cipher.workspace.DTOs;

public record AuthResult(
        String accessToken,
        String refreshToken,
        UserResponse user
) {}
