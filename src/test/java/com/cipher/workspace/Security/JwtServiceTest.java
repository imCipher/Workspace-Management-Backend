package com.cipher.workspace.Security;

import com.cipher.workspace.Entity.Role;
import com.cipher.workspace.Entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String TEST_SECRET = "5145c4bbe2eb855c489c178ad95a6d32ac3cc53ab383d4ba16dc8537831e9a0dcfe5ec01f9421e7786eaccd59a544a8197daaa4d0a9acf01e1a8a057e62f49ca";
    private static final String TEST_ISSUER = "workspacemanagement";
    private static final long EXPIRATION_MS = 900000; // 15 mins

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, TEST_ISSUER, EXPIRATION_MS);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("johndoe")
                .fullname("John Doe")
                .email("johndoe@workspace.local")
                .password("hashed_pwd")
                .role(Role.ADMIN)
                .build();
    }

    @Test
    @DisplayName("Should generate access token and successfully extract all standard and custom claims")
    void generateAccessToken_and_extractClaims() {
        String token = jwtService.generateAccessToken(testUser);

        assertNotNull(token);
        assertFalse(token.isBlank());

        assertEquals("johndoe", jwtService.extractUsername(token));
        assertEquals(testUser.getId(), jwtService.extractUserId(token));
        assertEquals("ADMIN", jwtService.extractRole(token));
        assertEquals("johndoe@workspace.local", jwtService.extractEmail(token));
        assertEquals("John Doe", jwtService.extractFullname(token));

        String tokenId = jwtService.extractTokenId(token);
        assertNotNull(tokenId);
        assertFalse(tokenId.isBlank());

        List<String> roles = jwtService.extractRoles(token);
        assertNotNull(roles);
        assertTrue(roles.contains("ROLE_ADMIN"));

        assertNotNull(jwtService.extractIssuedAt(token));
        assertNotNull(jwtService.extractExpiration(token));
        assertTrue(jwtService.extractExpiration(token).after(jwtService.extractIssuedAt(token)));
    }

    @Test
    @DisplayName("Should include extra custom claims when provided")
    void generateAccessToken_withExtraClaims() {
        Map<String, Object> extra = Map.of("department", "Engineering", "level", 3);
        String token = jwtService.generateAccessToken(testUser, extra);

        assertEquals("Engineering", jwtService.extractClaim(token, c -> c.get("department", String.class)));
        assertEquals(Integer.valueOf(3), jwtService.<Integer>extractClaim(token, c -> c.get("level", Integer.class)));
    }

    @Test
    @DisplayName("Should return true when validating token with matching user details")
    void isValid_withMatchingUser_returnsTrue() {
        String token = jwtService.generateAccessToken(testUser);
        assertTrue(jwtService.isValid(token, testUser));
        assertTrue(jwtService.isTokenValid(token, testUser));
    }

    @Test
    @DisplayName("Should return false when validating token with mismatched user details")
    void isValid_withDifferentUser_returnsFalse() {
        String token = jwtService.generateAccessToken(testUser);

        User anotherUser = User.builder()
                .username("janedoe")
                .build();

        assertFalse(jwtService.isValid(token, anotherUser));
    }

    @Test
    @DisplayName("Should return false for malformed or tampered token without throwing unhandled exceptions")
    void isValid_withMalformedToken_returnsFalse() {
        assertFalse(jwtService.isValid("invalid.jwt.token", testUser));
        assertFalse(jwtService.validateToken("invalid.jwt.token"));
    }

    @Test
    @DisplayName("Should return false when token has expired")
    void isValid_withExpiredToken_returnsFalse() {
        JwtService expiredService = new JwtService(TEST_SECRET, TEST_ISSUER, -1000);
        String expiredToken = expiredService.generateAccessToken(testUser);

        assertFalse(jwtService.isValid(expiredToken, testUser));
        assertFalse(jwtService.validateToken(expiredToken));
        assertTrue(jwtService.isTokenExpired(expiredToken));
    }

    @Test
    @DisplayName("Should return false when token issuer does not match")
    void isValid_withWrongIssuer_returnsFalse() {
        JwtService otherIssuerService = new JwtService(TEST_SECRET, "different-issuer", EXPIRATION_MS);
        String token = otherIssuerService.generateAccessToken(testUser);

        assertFalse(jwtService.isValid(token, testUser));
        assertFalse(jwtService.validateToken(token));
    }

    @Test
    @DisplayName("JwtProvider should inherit all functionality and work identically")
    void jwtProvider_worksIdentically() {
        JwtProvider provider = new JwtProvider(TEST_SECRET, TEST_ISSUER, EXPIRATION_MS);
        String token = provider.generateAccessToken(testUser);

        assertTrue(provider.isValid(token, testUser));
        assertEquals("johndoe", provider.extractUsername(token));
    }
}
