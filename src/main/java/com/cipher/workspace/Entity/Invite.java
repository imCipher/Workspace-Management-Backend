package com.cipher.workspace.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "invites",
        indexes = {
                @Index(name = "idx_invites_token_hash", columnList = "token_hash"),
                @Index(name = "idx_invites_email", columnList = "email")
        }
)
public class Invite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column
    private String fullname;

    @Column(name = "token_hash", length = 255)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invited_by")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User invitedBy;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role = Role.WORKER;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean revoked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public boolean isValid() {
        return !revoked && usedAt == null && !isExpired();
    }
}
