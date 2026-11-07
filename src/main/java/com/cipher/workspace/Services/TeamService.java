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
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Repository.InviteRepository;
import com.cipher.workspace.Repository.RefreshTokenRepository;
import com.cipher.workspace.Repository.UserRepository;
import com.cipher.workspace.Repository.VisitLogsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private final UserRepository userRepository;
    private final InviteRepository inviteRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VisitLogsRepository visitLogsRepository;
    private final EmailService emailService;
    private final EmailProperties emailProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public TeamService(
            UserRepository userRepository,
            InviteRepository inviteRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenRepository refreshTokenRepository,
            VisitLogsRepository visitLogsRepository,
            EmailService emailService,
            EmailProperties emailProperties
    ) {
        this.userRepository = userRepository;
        this.inviteRepository = inviteRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.visitLogsRepository = visitLogsRepository;
        this.emailService = emailService;
        this.emailProperties = emailProperties;
    }

    /**
     * Invites a new team member.
     * Supports both:
     * - Standard invitation link flow (user receives email and configures their own username & password).
     * - Instant provisioning (user is created immediately with auto-generated credentials and emailed login details).
     */
    @Transactional
    public InviteResponse inviteWorker(User adminUser, InviteWorkerRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }

        String email = request.getEmail().trim().toLowerCase();
        String fullname = request.getFullname() != null ? request.getFullname().trim() : email;
        Role role = request.getRole() != null ? request.getRole() : Role.WORKER;

        User admin = adminUser != null && adminUser.getId() != null
                ? userRepository.findById(adminUser.getId()).orElse(adminUser)
                : adminUser;

        // Ensure user does not already exist as active
        userRepository.findByEmail(email).ifPresent(existingUser -> {
            if (existingUser.getStatus() == UserStatus.ACTIVE) {
                throw new DuplicateResourceException("A user with email '" + email + "' already exists in this workspace");
            }
        });

        // Revoke any previous unaccepted invites for this email
        inviteRepository.findByEmailAndRevokedFalse(email).ifPresent(prevInvite -> {
            prevInvite.setRevoked(true);
            inviteRepository.save(prevInvite);
        });

        return createInviteFlow(admin, email, fullname, role);
    }

    private InviteResponse createInviteFlow(User admin, String email, String fullname, Role role) {
        String rawToken = generateSecureToken();
        String tokenHash = AuthService.hashToken(rawToken);

        // Provision unactivated user in PENDING_INVITE status
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setFullname(fullname);
        user.setEmail(email);
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            user.setUsername("pending_" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        }
        user.setRole(role);
        user.setStatus(UserStatus.PENDING_INVITE);
        user.setEmailVerified(true);
        user.setInvitedBy(admin);

        userRepository.save(user);

        Instant expiresAt = Instant.now().plus(emailProperties.getTokenExpirationHours(), ChronoUnit.HOURS);

        Invite invite = Invite.builder()
                .email(email)
                .fullname(fullname)
                .tokenHash(tokenHash)
                .invitedBy(admin)
                .role(role)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        Invite savedInvite = inviteRepository.save(invite);

        String separator = emailProperties.getInviteUrl().contains("?") ? "&" : "?";
        String inviteUrl = emailProperties.getInviteUrl() + separator + "token=" + rawToken;

        emailService.sendInviteEmail(email, fullname, role, rawToken);

        return InviteResponse.from(savedInvite, inviteUrl);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TeamMemberResponse> getTeamMembers(int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = Math.min(100, Math.max(1, size));
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> pageResult = userRepository.findAll(pageable);
        return PagedResponse.from(pageResult.map(TeamMemberResponse::from));
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeamMembers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(TeamMemberResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<TeamMemberResponse> searchTeamMembers(Role role, UserStatus status, String search, int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = Math.min(100, Math.max(1, size));
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<User> pageResult = userRepository.searchMembers(role, status, search, pageable);
        return PagedResponse.from(pageResult.map(TeamMemberResponse::from));
    }

    @Transactional(readOnly = true)
    public PagedResponse<InviteResponse> getInvites(int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = Math.min(100, Math.max(1, size));
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Invite> pageResult = inviteRepository.findAll(pageable);
        return PagedResponse.from(pageResult.map(InviteResponse::from));
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getInvites() {
        return inviteRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(InviteResponse::from)
                .toList();
    }

    /**
     * Removes or deactivates a team member or revokes an invite.
     * Enforces soft deactivation (status = INACTIVE) when the user has audit references in visit_logs.
     */
    @Transactional
    public void removeWorkerOrInvite(User adminUser, UUID id) {
        if (id == null) {
            throw new BadRequestException("Target ID cannot be null");
        }

        // 1. Try finding by Invite ID
        Optional<Invite> inviteOpt = inviteRepository.findById(id);
        if (inviteOpt.isPresent()) {
            Invite invite = inviteOpt.get();
            invite.setRevoked(true);
            inviteRepository.save(invite);

            if (invite.getEmail() != null) {
                userRepository.findByEmail(invite.getEmail()).ifPresent(user -> {
                    if (user.getStatus() == UserStatus.PENDING_INVITE) {
                        deactivateOrDeleteUser(user);
                    }
                });
            }
            return;
        }

        // 2. Try finding by User ID
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User worker = userOpt.get();
            if (adminUser != null && worker.getId().equals(adminUser.getId())) {
                throw new BadRequestException("Admin cannot remove their own account");
            }

            // Revoke any active invites for this email
            if (worker.getEmail() != null) {
                inviteRepository.findByEmailAndRevokedFalse(worker.getEmail()).ifPresent(inv -> {
                    inv.setRevoked(true);
                    inviteRepository.save(inv);
                });
            }

            deactivateOrDeleteUser(worker);
            return;
        }

        throw new ResourceNotFoundException("Team member or invite not found with ID: " + id);
    }

    @Transactional
    public void revokeInvite(User adminUser, UUID inviteId) {
        removeWorkerOrInvite(adminUser, inviteId);
    }

    @Transactional
    public void removeTeamMember(User adminUser, UUID memberId) {
        removeWorkerOrInvite(adminUser, memberId);
    }

    private void deactivateOrDeleteUser(User worker) {
        // Invalidate active sessions
        try {
            refreshTokenRepository.deleteAllByUser(worker);
        } catch (Exception ignored) {
            refreshTokenRepository.revokeAllByUser(worker, Instant.now());
        }

        // Soft deactivation check: if the user is referenced in visit_logs or expenses, preserve audit history
        boolean hasVisitLogs = visitLogsRepository.existsByAnyUserReference(worker.getId());

        if (hasVisitLogs) {
            worker.setStatus(UserStatus.INACTIVE);
            userRepository.save(worker);
            log.info("Soft-deactivated user {} (status=INACTIVE) due to historical visit log references", worker.getId());
        } else {
            try {
                userRepository.delete(worker);
                log.info("Deleted user {} successfully", worker.getId());
            } catch (Exception ex) {
                worker.setStatus(UserStatus.INACTIVE);
                userRepository.save(worker);
                log.info("Fell back to soft-deactivation for user {} (status=INACTIVE)", worker.getId());
            }
        }
    }

    public String generateRandomUsernameFromEmail(String email) {
        String base = "worker";
        if (email != null && email.contains("@")) {
            String prefix = email.substring(0, email.indexOf('@')).toLowerCase().trim();
            if (prefix.contains("+")) {
                prefix = prefix.substring(0, prefix.indexOf('+'));
            }
            String cleaned = prefix.replaceAll("[^a-z0-9]", "");
            if (!cleaned.isBlank()) {
                base = cleaned.length() > 12 ? cleaned.substring(0, 12) : cleaned;
            }
        }

        for (int i = 0; i < 10; i++) {
            int randomDigits = 1000 + secureRandom.nextInt(9000);
            String candidate = base + "_" + randomDigits;
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }

        byte[] hexBytes = new byte[3];
        secureRandom.nextBytes(hexBytes);
        return base + "_" + HexFormat.of().formatHex(hexBytes);
    }


    private static String generateSecureToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }
}
