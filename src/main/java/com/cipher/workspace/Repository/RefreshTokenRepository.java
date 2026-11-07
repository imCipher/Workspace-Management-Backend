package com.cipher.workspace.Repository;

import com.cipher.workspace.Entity.RefreshToken;
import com.cipher.workspace.Entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r
        FROM RefreshToken r
        WHERE r.tokenHash = :tokenHash
    """)
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE RefreshToken r
        SET r.revoked = true,
            r.revokedAt = :revokedAt
        WHERE r.familyId = :familyId
          AND r.revoked = false
    """)
    int revokeActiveByFamilyId(
            @Param("familyId") UUID familyId,
            @Param("revokedAt") Instant revokedAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE RefreshToken r
        SET r.revoked = true,
            r.revokedAt = :revokedAt
        WHERE r.user = :user
          AND r.revoked = false
    """)
    int revokeAllByUser(
            @Param("user") User user,
            @Param("revokedAt") Instant revokedAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE RefreshToken r
        SET r.revoked = true,
            r.revokedAt = :revokedAt
        WHERE r.user.id = :userId
          AND r.revoked = false
    """)
    int revokeAllByUserId(
            @Param("userId") UUID userId,
            @Param("revokedAt") Instant revokedAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        DELETE FROM RefreshToken r
        WHERE r.user = :user
    """)
    int deleteAllByUser(@Param("user") User user);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        DELETE FROM RefreshToken r
        WHERE r.user.id = :userId
    """)
    int deleteAllByUserId(@Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        DELETE FROM RefreshToken r
        WHERE r.expiresAt < :cutoff
    """)
    int deleteExpiredTokens(@Param("cutoff") Instant cutoff);
}
