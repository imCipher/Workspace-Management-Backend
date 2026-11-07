package com.cipher.workspace.Services;

import com.cipher.workspace.Config.EmailProperties;
import com.cipher.workspace.DTOs.InviteResponse;
import com.cipher.workspace.DTOs.InviteWorkerRequest;
import com.cipher.workspace.DTOs.PagedResponse;
import com.cipher.workspace.DTOs.TeamMemberResponse;
import com.cipher.workspace.Entity.Invite;
import com.cipher.workspace.Entity.Role;
import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Entity.UserStatus;
import com.cipher.workspace.Exception.BadRequestException;
import com.cipher.workspace.Exception.DuplicateResourceException;
import com.cipher.workspace.Repository.InviteRepository;
import com.cipher.workspace.Repository.RefreshTokenRepository;
import com.cipher.workspace.Repository.UserRepository;
import com.cipher.workspace.Repository.VisitLogsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private InviteRepository inviteRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private VisitLogsRepository visitLogsRepository;
    @Mock
    private EmailService emailService;

    private EmailProperties emailProperties;
    private TeamService teamService;
    private User admin;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailProperties.setInviteUrl("http://localhost:3000/accept-invite");
        emailProperties.setTokenExpirationHours(48);

        teamService = new TeamService(
                userRepository,
                inviteRepository,
                passwordEncoder,
                refreshTokenRepository,
                visitLogsRepository,
                emailService,
                emailProperties
        );

        admin = User.builder()
                .id(UUID.randomUUID())
                .username("admin1")
                .fullname("System Admin")
                .email("admin@workspace.local")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("inviteWorker (link flow) should provision user in PENDING_INVITE and dispatch invite email")
    void inviteWorker_linkFlow_success() {
        InviteWorkerRequest request = InviteWorkerRequest.builder()
                .email("worker@workspace.local")
                .fullname("Jane Worker")
                .role(Role.WORKER)
                .build();

        when(userRepository.findByEmail("worker@workspace.local")).thenReturn(Optional.empty());
        when(inviteRepository.findByEmailAndRevokedFalse("worker@workspace.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPlaceholder");
        when(inviteRepository.save(any(Invite.class))).thenAnswer(i -> {
            Invite inv = i.getArgument(0);
            inv.setId(UUID.randomUUID());
            return inv;
        });

        InviteResponse response = teamService.inviteWorker(admin, request);

        assertNotNull(response);
        assertEquals("worker@workspace.local", response.email());
        assertEquals(Role.WORKER, response.role());
        assertNotNull(response.inviteUrl());
        assertTrue(response.inviteUrl().contains("token="));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(UserStatus.PENDING_INVITE, savedUser.getStatus());
        assertEquals("worker@workspace.local", savedUser.getEmail());

        verify(emailService).sendInviteEmail(eq("worker@workspace.local"), eq("Jane Worker"), eq(Role.WORKER), anyString());
    }

    @Test
    @DisplayName("inviteWorker should throw DuplicateResourceException if active user already exists")
    void inviteWorker_activeUserExists_throwsException() {
        InviteWorkerRequest request = InviteWorkerRequest.builder()
                .email("existing@workspace.local")
                .fullname("Existing User")
                .build();

        User existing = User.builder()
                .email("existing@workspace.local")
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("existing@workspace.local")).thenReturn(Optional.of(existing));

        assertThrows(DuplicateResourceException.class, () -> teamService.inviteWorker(admin, request));
    }

    @Test
    @DisplayName("getTeamMembers should return paged responses")
    void getTeamMembers_paged() {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(admin)));

        PagedResponse<TeamMemberResponse> result = teamService.getTeamMembers(0, 10);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("admin1", result.content().get(0).username());
    }

    @Test
    @DisplayName("removeWorkerOrInvite should soft-deactivate user when audit references exist in visit_logs")
    void removeTeamMember_softDeactivatesWhenVisitLogsExist() {
        User worker = User.builder()
                .id(UUID.randomUUID())
                .username("worker1")
                .email("worker@workspace.local")
                .status(UserStatus.ACTIVE)
                .build();

        when(inviteRepository.findById(worker.getId())).thenReturn(Optional.empty());
        when(userRepository.findById(worker.getId())).thenReturn(Optional.of(worker));
        when(visitLogsRepository.existsByAnyUserReference(worker.getId())).thenReturn(true);

        teamService.removeTeamMember(admin, worker.getId());

        assertEquals(UserStatus.INACTIVE, worker.getStatus());
        verify(userRepository).save(worker);
        verify(userRepository, never()).delete(worker);
        verify(refreshTokenRepository).deleteAllByUser(worker);
    }

    @Test
    @DisplayName("removeWorkerOrInvite should hard-delete user when no audit references exist")
    void removeTeamMember_hardDeletesWhenNoAuditReferences() {
        User worker = User.builder()
                .id(UUID.randomUUID())
                .username("worker2")
                .email("worker2@workspace.local")
                .status(UserStatus.ACTIVE)
                .build();

        when(inviteRepository.findById(worker.getId())).thenReturn(Optional.empty());
        when(userRepository.findById(worker.getId())).thenReturn(Optional.of(worker));
        when(visitLogsRepository.existsByAnyUserReference(worker.getId())).thenReturn(false);

        teamService.removeTeamMember(admin, worker.getId());

        verify(userRepository).delete(worker);
    }

    @Test
    @DisplayName("removeWorkerOrInvite should prevent admin from removing their own account")
    void removeTeamMember_adminCannotRemoveSelf() {
        when(inviteRepository.findById(admin.getId())).thenReturn(Optional.empty());
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThrows(BadRequestException.class, () -> teamService.removeTeamMember(admin, admin.getId()));
    }

    @Test
    @DisplayName("revokeInvite should set invite revoked to true")
    void revokeInvite_succeeds() {
        UUID inviteId = UUID.randomUUID();
        Invite invite = Invite.builder()
                .id(inviteId)
                .email("pending@workspace.local")
                .revoked(false)
                .build();

        when(inviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));
        when(userRepository.findByEmail("pending@workspace.local")).thenReturn(Optional.empty());

        teamService.revokeInvite(admin, inviteId);

        assertTrue(invite.isRevoked());
        verify(inviteRepository).save(invite);
    }
}
