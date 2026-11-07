package com.cipher.workspace.Services;

import com.cipher.workspace.Entity.RefreshToken;
import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Exception.InvalidTokenException;
import com.cipher.workspace.Repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final long refreshExpirationMs;
    private final boolean ipBindingEnabled;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository repository,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs,
            @Value("${app.security.ip-binding-enabled:false}") boolean ipBindingEnabled
    ) {
        this.repository = repository;
        this.refreshExpirationMs = refreshExpirationMs;
        this.ipBindingEnabled = ipBindingEnabled;
    }

    /**
     * Creates a new session and token family for the user upon sign-in.
     */
    @Transactional
    public String createSession(User user, String clientIp) {
        UUID familyId = UUID.randomUUID();
        return createToken(user, familyId, clientIp).rawToken();
    }

    /**
     * Atomically rotates a refresh token.
     * Prevents token reuse: if a previously revoked or used token is presented,
     * the entire token family is immediately revoked and an InvalidTokenException is thrown.
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public RotationResult rotate(String rawToken, String currentIp) {
        validateTokenPresent(rawToken);

        String tokenHash = hashToken(rawToken);
        Instant now = Instant.now();

        RefreshToken currentToken = repository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (currentToken.isRevoked() || currentToken.getUsedAt() != null) {
            handleReuse(currentToken, now);
            throw new InvalidTokenException("Invalid refresh token");
        }

        if (!currentToken.getExpiresAt().isAfter(now)) {
            currentToken.setRevoked(true);
            currentToken.setRevokedAt(now);
            repository.saveAndFlush(currentToken);
            throw new InvalidTokenException("Refresh token has expired");
        }

        if (ipBindingEnabled && currentToken.getCreatedIp() != null && !currentToken.getCreatedIp().equals(currentIp)) {
            handleIpMismatch(currentToken, currentIp, now);
            throw new InvalidTokenException("Invalid refresh token");
        }

        IssuedToken replacement = createTokenEntity(
                currentToken.getUser(),
                currentToken.getFamilyId(),
                currentToken.getCreatedIp() != null ? currentToken.getCreatedIp() : currentIp
        );

        currentToken.setRevoked(true);
        currentToken.setUsedAt(now);
        currentToken.setRevokedAt(now);
        currentToken.setReplacedByTokenHash(replacement.entity().getTokenHash());

        repository.save(currentToken);
        repository.save(replacement.entity());
        repository.flush();

        return new RotationResult(replacement.rawToken(), currentToken.getUser());
    }

    @Transactional(readOnly = true)
    public RefreshToken verify(String rawToken) {
        validateTokenPresent(rawToken);

        RefreshToken token = repository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (token.isRevoked() || token.getUsedAt() != null) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        if (!token.getExpiresAt().isAfter(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        return token;
    }

    @Transactional
    public void revokeByToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        String tokenHash = hashToken(rawToken);
        Instant now = Instant.now();

        repository.findByTokenHashForUpdate(tokenHash).ifPresent(token -> {
            if (!token.isRevoked()) {
                token.setRevoked(true);
                token.setRevokedAt(now);
                repository.save(token);
            }
        });
    }

    @Transactional
    public int revokeAllForUser(User user) {
        return repository.revokeAllByUser(user, Instant.now());
    }

    private void handleReuse(RefreshToken reusedToken, Instant detectedAt) {
        int revokedCount = repository.revokeActiveByFamilyId(reusedToken.getFamilyId(), detectedAt);
        log.warn("Refresh-token reuse detected; entire family revoked. familyId={}, userId={}, activeRevoked={}",
                reusedToken.getFamilyId(),
                reusedToken.getUser() != null ? reusedToken.getUser().getId() : "null",
                revokedCount);
    }

    private void handleIpMismatch(RefreshToken token, String attemptedIp, Instant detectedAt) {
        int revokedCount = repository.revokeActiveByFamilyId(token.getFamilyId(), detectedAt);
        log.warn("Refresh-token IP mismatch; family revoked. familyId={}, userId={}, createdIp={}, attemptedIp={}, activeRevoked={}",
                token.getFamilyId(),
                token.getUser() != null ? token.getUser().getId() : "null",
                token.getCreatedIp(),
                attemptedIp,
                revokedCount);
    }

    private IssuedToken createToken(User user, UUID familyId, String clientIp) {
        IssuedToken issuedToken = createTokenEntity(user, familyId, clientIp);
        repository.save(issuedToken.entity());
        return issuedToken;
    }

    private IssuedToken createTokenEntity(User user, UUID familyId, String clientIp) {
        String rawToken = generateSecureToken();

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .familyId(familyId)
                .createdIp(clientIp)
                .tokenHash(hashToken(rawToken))
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();

        return new IssuedToken(rawToken, entity);
    }

    private void validateTokenPresent(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidTokenException("Missing refresh token");
        }
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private record IssuedToken(String rawToken, RefreshToken entity) {}

    public record RotationResult(String refreshToken, User user) {}
}
