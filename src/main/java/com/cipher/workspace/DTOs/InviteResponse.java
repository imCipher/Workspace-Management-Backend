package com.cipher.workspace.DTOs;

import com.cipher.workspace.Entity.Invite;
import com.cipher.workspace.Entity.Role;

import java.time.Instant;
import java.util.UUID;

public record InviteResponse(
        UUID id,
        String email,
        String fullname,
        Role role,
        String invitedByUsername,
        Instant expiresAt,
        boolean valid,
        String inviteUrl
) {
    public InviteResponse(UUID id, String email, String fullname, Role role, String invitedByUsername, Instant expiresAt, boolean valid) {
        this(id, email, fullname, role, invitedByUsername, expiresAt, valid, null);
    }

    public static InviteResponse from(Invite invite) {
        return from(invite, null);
    }

    public static InviteResponse from(Invite invite, String inviteUrl) {
        if (invite == null) {
            return null;
        }
        return new InviteResponse(
                invite.getId(),
                invite.getEmail(),
                invite.getFullname(),
                invite.getRole(),
                invite.getInvitedBy() != null ? invite.getInvitedBy().getUsername() : null,
                invite.getExpiresAt(),
                invite.isValid(),
                inviteUrl
        );
    }
}
