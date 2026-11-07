package com.cipher.workspace.DTOs;

import com.cipher.workspace.Entity.Role;
import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Entity.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullname,
        String username,
        String email,
        Role role,
        UserStatus status,
        UUID invitedById,
        boolean emailVerified,
        LocalDateTime createdAt
) {

    public UserResponse(UUID id, String fullname, String username, Role role) {
        this(id, fullname, username, null, role, UserStatus.ACTIVE, null, true, null);
    }

    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getFullname(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getInvitedBy() != null ? user.getInvitedBy().getId() : null,
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}
