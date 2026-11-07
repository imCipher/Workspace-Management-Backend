package com.cipher.workspace.Repository;

import com.cipher.workspace.Entity.Invite;
import com.cipher.workspace.Entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InviteRepository extends JpaRepository<Invite, UUID> {

    Optional<Invite> findByTokenHash(String tokenHash);

    Optional<Invite> findByEmail(String email);

    Optional<Invite> findByEmailAndRevokedFalse(String email);

    Optional<Invite> findByEmailAndRevokedFalseAndUsedAtIsNull(String email);

    boolean existsByEmailAndRevokedFalseAndUsedAtIsNull(String email);

    List<Invite> findByRevokedFalse();

    Page<Invite> findByRevokedFalse(Pageable pageable);

    List<Invite> findByRole(Role role);

    Page<Invite> findByRole(Role role, Pageable pageable);

    List<Invite> findByInvitedById(UUID invitedById);

    Page<Invite> findByInvitedById(UUID invitedById, Pageable pageable);

    Page<Invite> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
