package com.cipher.workspace.Repository;

import com.cipher.workspace.Entity.PasswordResetToken;
import com.cipher.workspace.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findByUserAndRevokedFalse(User user);

    List<PasswordResetToken> findByUser(User user);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE PasswordResetToken t
        SET t.revoked = true
        WHERE t.user = :user AND t.revoked = false
    """)
    int revokeAllByUser(@Param("user") User user);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM PasswordResetToken t WHERE t.user = :user")
    int deleteAllByUser(@Param("user") User user);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :cutoff")
    int deleteExpiredTokens(@Param("cutoff") Instant cutoff);
}
