package com.cipher.workspace.DTOs;

public record TokenPair(
        String accessToken,
        String refreshToken
) {}
