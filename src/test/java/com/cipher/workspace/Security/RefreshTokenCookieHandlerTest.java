package com.cipher.workspace.Security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenCookieHandlerTest {

    private RefreshTokenCookieHandler cookieHandler;

    @BeforeEach
    void setUp() {
        cookieHandler = new RefreshTokenCookieHandler(
                false,
                "/api/auth",
                "Strict",
                604800000L // 7 days
        );
    }

    @Test
    @DisplayName("Should create secure HttpOnly cookie with configured attributes")
    void createRefreshCookie_createsExpectedCookie() {
        String token = "sample-refresh-token-value";
        ResponseCookie cookie = cookieHandler.createRefreshCookie(token);

        assertEquals("refreshToken", cookie.getName());
        assertEquals(token, cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertFalse(cookie.isSecure());
        assertEquals("/api/auth", cookie.getPath());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals(604800, cookie.getMaxAge().getSeconds());
    }

    @Test
    @DisplayName("Should create clear cookie with maxAge 0 to invalidate client cookie")
    void createClearCookie_createsZeroMaxAgeCookie() {
        ResponseCookie cookie = cookieHandler.createClearCookie();

        assertEquals("refreshToken", cookie.getName());
        assertEquals("", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/api/auth", cookie.getPath());
        assertEquals(0, cookie.getMaxAge().getSeconds());
    }

    @Test
    @DisplayName("Should extract refresh token from HttpServletRequest cookies")
    void extractRefreshToken_fromCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("otherCookie", "abc"), new Cookie("refreshToken", "my-secret-token"));

        Optional<String> token = cookieHandler.extractRefreshToken(request);

        assertTrue(token.isPresent());
        assertEquals("my-secret-token", token.get());
    }

    @Test
    @DisplayName("Should fall back to extracting refresh token from X-Refresh-Token header")
    void extractRefreshToken_fromHeaderFallback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Refresh-Token", "header-token-123");

        Optional<String> token = cookieHandler.extractRefreshToken(request);

        assertTrue(token.isPresent());
        assertEquals("header-token-123", token.get());
    }

    @Test
    @DisplayName("Should return empty when neither cookie nor header is provided")
    void extractRefreshToken_absentReturnsEmpty() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Optional<String> token = cookieHandler.extractRefreshToken(request);

        assertTrue(token.isEmpty());
    }
}
