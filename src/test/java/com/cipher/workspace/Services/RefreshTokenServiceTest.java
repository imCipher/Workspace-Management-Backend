package com.cipher.workspace.Services;

import com.cipher.workspace.Entity.RefreshToken;
import com.cipher.workspace.Entity.Role;
import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Exception.InvalidTokenException;
import com.cipher.workspace.Repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;
    private User testUser;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, 604800000L, false);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("worker1")
                .role(Role.WORKER)
                .build();
    }

    @Test
    @DisplayName("createSession should generate secure token and persist entity")
    void createSession_generatesTokenAndPersists() {
        String token = service.createSession(testUser, "127.0.0.1");

        assertNotNull(token);
        assertFalse(token.isBlank());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertEquals(testUser, saved.getUser());
        assertEquals("127.0.0.1", saved.getCreatedIp());
        assertNotNull(saved.getFamilyId());
        assertNotNull(saved.getTokenHash());
        assertFalse(saved.isRevoked());
    }

    @Test
    @DisplayName("rotate should succeed for active token, revoking old and issuing replacement")
    void rotate_succeedsForActiveToken() {
        UUID familyId = UUID.randomUUID();
        String initialToken = service.createSession(testUser, "127.0.0.1");

        ArgumentCaptor<RefreshToken> createCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(createCaptor.capture());
        RefreshToken existingToken = createCaptor.getValue();
        existingToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(repository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(existingToken));

        RefreshTokenService.RotationResult result = service.rotate(initialToken, "127.0.0.1");

        assertNotNull(result);
        assertNotNull(result.refreshToken());
        assertNotEquals(initialToken, result.refreshToken());
        assertEquals(testUser, result.user());

        assertTrue(existingToken.isRevoked());
        assertNotNull(existingToken.getUsedAt());
        assertNotNull(existingToken.getReplacedByTokenHash());
    }

    @Test
    @DisplayName("rotate should detect reuse and revoke entire family when revoked token is used")
    void rotate_reuseDetected_revokesFamily() {
        UUID familyId = UUID.randomUUID();
        RefreshToken reusedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .familyId(familyId)
                .revoked(true)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(repository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(reusedToken));

        assertThrows(InvalidTokenException.class, () -> service.rotate("reused-raw-token", "127.0.0.1"));

        verify(repository).revokeActiveByFamilyId(eq(familyId), any(Instant.class));
    }

    @Test
    @DisplayName("rotate should fail and mark token revoked when expired")
    void rotate_expiredToken_throwsInvalidTokenException() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .familyId(UUID.randomUUID())
                .revoked(false)
                .expiresAt(Instant.now().minusSeconds(10))
                .build();

        when(repository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(expiredToken));

        assertThrows(InvalidTokenException.class, () -> service.rotate("expired-raw-token", "127.0.0.1"));

        assertTrue(expiredToken.isRevoked());
        verify(repository).saveAndFlush(expiredToken);
    }

    @Test
    @DisplayName("verify should return token when valid and unrevoked")
    void verify_validToken_returnsToken() {
        RefreshToken validToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .familyId(UUID.randomUUID())
                .revoked(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(repository.findByTokenHash(any())).thenReturn(Optional.of(validToken));

        RefreshToken result = service.verify("raw-token");
        assertEquals(validToken, result);
    }

    @Test
    @DisplayName("revokeByToken should set revoked = true")
    void revokeByToken_revokesToken() {
        RefreshToken token = RefreshToken.builder()
                .id(UUID.randomUUID())
                .revoked(false)
                .build();

        when(repository.findByTokenHashForUpdate(any())).thenReturn(Optional.of(token));

        service.revokeByToken("raw-token");

        assertTrue(token.isRevoked());
        assertNotNull(token.getRevokedAt());
        verify(repository).save(token);
    }

    @Test
    @DisplayName("revokeAllForUser should invoke repository.revokeAllByUser")
    void revokeAllForUser_delegatesToRepository() {
        when(repository.revokeAllByUser(eq(testUser), any(Instant.class))).thenReturn(3);

        int count = service.revokeAllForUser(testUser);
        assertEquals(3, count);
    }
}
