package com.cipher.workspace.Security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class RefreshTokenCookieHandler {

    public static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final boolean secureCookie;
    private final String cookiePath;
    private final Duration refreshDuration;
    private final String sameSite;

    public RefreshTokenCookieHandler(
            @Value("${app.cookie.secure:false}") boolean secureCookie,
            @Value("${app.cookie.path:/api/auth}") String cookiePath,
            @Value("${app.cookie.same-site:Strict}") String sameSite,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs
    ) {
        this.secureCookie = secureCookie;
        this.cookiePath = cookiePath;
        this.sameSite = sameSite;
        this.refreshDuration = Duration.ofMillis(refreshExpirationMs);
    }

    /**
     * Generates a secure, HttpOnly ResponseCookie containing the raw refresh token.
     */
    public ResponseCookie createRefreshCookie(String rawRefreshToken) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, rawRefreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path(cookiePath)
                .maxAge(refreshDuration)
                .build();
    }

    /**
     * Generates a cookie invalidation directive (maxAge = 0) to clear the refresh token.
     */
    public ResponseCookie createClearCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path(cookiePath)
                .maxAge(0)
                .build();
    }

    /**
     * Extracts the refresh token from HttpServletRequest cookies, with fallback to X-Refresh-Token header.
     */
    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }

        if (request.getCookies() != null) {
            Optional<String> fromCookie = Arrays.stream(request.getCookies())
                    .filter(c -> REFRESH_COOKIE_NAME.equals(c.getName()))
                    .map(Cookie::getValue)
                    .filter(v -> v != null && !v.isBlank())
                    .findFirst();

            if (fromCookie.isPresent()) {
                return fromCookie;
            }
        }

        String fromHeader = request.getHeader("X-Refresh-Token");
        if (fromHeader != null && !fromHeader.isBlank()) {
            return Optional.of(fromHeader.trim());
        }

        return Optional.empty();
    }

    public String getCookieName() {
        return REFRESH_COOKIE_NAME;
    }

    public String getCookiePath() {
        return cookiePath;
    }

    public boolean isSecureCookie() {
        return secureCookie;
    }
}
