package com.cipher.workspace.DTOs;

import com.cipher.workspace.Entity.Role;
import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Entity.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TeamMemberResponse(
        UUID id,
        String fullname,
        String username,
        String email,
        Role role,
        UserStatus status,
        UUID invitedById,
        String invitedByName,
        LocalDateTime createdAt
) {
    public static TeamMemberResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new TeamMemberResponse(
                user.getId(),
                user.getFullname(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getInvitedBy() != null ? user.getInvitedBy().getId() : null,
                user.getInvitedBy() != null ? user.getInvitedBy().getFullname() : null,
                user.getCreatedAt()
        );
    }
}
