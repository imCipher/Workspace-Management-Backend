package com.cipher.workspace.Services;

import com.cipher.workspace.Config.EmailProperties;
import com.cipher.workspace.DTOs.*;
import com.cipher.workspace.Entity.*;
import com.cipher.workspace.Exception.InvalidTokenException;
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Exception.UsernameAlreadyExistsException;
import com.cipher.workspace.Repository.*;
import com.cipher.workspace.Security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private InviteRepository inviteRepository;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private EmailService emailService;

    private EmailProperties emailProperties;
    private AuthService authService;

    private User activeAdmin;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailProperties.setTokenExpirationHours(48);
        emailProperties.setInviteUrl("http://localhost:3000/accept-invite");
        emailProperties.setResetPasswordUrl("http://localhost:3000/reset-password");
        emailProperties.setLoginUrl("http://localhost:3000/login");
        emailProperties.setSenderName("Workspace");

        authService = new AuthService(
                userRepository,
                inviteRepository,
                refreshTokenService,
                passwordResetTokenRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                emailService,
                emailProperties
        );

        activeAdmin = User.builder()
                .id(UUID.randomUUID())
                .username("admin1")
                .fullname("Admin User")
                .email("admin@workspace.local")
                .password("encoded_pass")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }

    @Test
    @DisplayName("signIn should succeed for active user")
    void signIn_success() {
        SignInRequest request = new SignInRequest("admin1", "raw_password");

        when(userRepository.findByUsernameOrEmail("admin1")).thenReturn(Optional.of(activeAdmin));
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(activeAdmin, "raw_password", activeAdmin.getAuthorities())
        );
        when(jwtService.generateAccessToken(activeAdmin)).thenReturn("access.jwt.token");
        when(refreshTokenService.createSession(activeAdmin, "127.0.0.1")).thenReturn("refresh-token-123");

        AuthResult result = authService.signIn(request, "127.0.0.1");

        assertNotNull(result);
        assertEquals("access.jwt.token", result.accessToken());
        assertEquals("refresh-token-123", result.refreshToken());
        assertEquals("admin1", result.user().username());
    }

    @Test
    @DisplayName("signIn should throw DisabledException when invitation is pending")
    void signIn_pendingInvite_throwsDisabledException() {
        activeAdmin.setStatus(UserStatus.PENDING_INVITE);
        SignInRequest request = new SignInRequest("admin1", "raw_password");

        when(userRepository.findByUsernameOrEmail("admin1")).thenReturn(Optional.of(activeAdmin));
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(activeAdmin, "raw_password", activeAdmin.getAuthorities())
        );

        DisabledException ex = assertThrows(DisabledException.class, () -> authService.signIn(request, "127.0.0.1"));
        assertTrue(ex.getMessage().contains("pending"));
    }

    @Test
    @DisplayName("signIn should throw DisabledException when account is inactive")
    void signIn_inactive_throwsDisabledException() {
        activeAdmin.setStatus(UserStatus.INACTIVE);
        SignInRequest request = new SignInRequest("admin1", "raw_password");

        when(userRepository.findByUsernameOrEmail("admin1")).thenReturn(Optional.of(activeAdmin));
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(activeAdmin, "raw_password", activeAdmin.getAuthorities())
        );

        DisabledException ex = assertThrows(DisabledException.class, () -> authService.signIn(request, "127.0.0.1"));
        assertTrue(ex.getMessage().contains("deactivated"));
    }

    @Test
    @DisplayName("signIn should throw BadCredentialsException when user not found")
    void signIn_unknownUser_throwsBadCredentialsException() {
        SignInRequest request = new SignInRequest("nonexistent", "secret");
        when(userRepository.findByUsernameOrEmail("nonexistent")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.signIn(request, "127.0.0.1"));
    }

    @Test
    @DisplayName("acceptInvite should allow invited staff to set username and password, activate account, and sign in")
    void acceptInvite_success() {
        String rawToken = "raw-invite-token";
        String tokenHash = AuthService.hashToken(rawToken);

        Invite invite = Invite.builder()
                .id(UUID.randomUUID())
                .email("worker@workspace.local")
                .fullname("New Worker")
                .role(Role.WORKER)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                .revoked(false)
                .build();

        when(inviteRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(invite));
        when(userRepository.findByEmail("worker@workspace.local")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("worker1")).thenReturn(false);
        when(passwordEncoder.encode("secretPass")).thenReturn("encodedSecretPass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("worker-access-jwt");
        when(refreshTokenService.createSession(any(User.class), eq("127.0.0.1"))).thenReturn("worker-refresh-token");

        AcceptInviteRequest request = AcceptInviteRequest.builder()
                .token(rawToken)
                .username("worker1")
                .fullname("New Worker")
                .password("secretPass")
                .build();

        AuthResult result = authService.acceptInvite(request, "127.0.0.1");

        assertNotNull(result);
        assertEquals("worker-access-jwt", result.accessToken());
        assertEquals("worker-refresh-token", result.refreshToken());
        assertEquals("worker1", result.user().username());
        assertNotNull(invite.getUsedAt());

        verify(inviteRepository).save(invite);
        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail(eq("worker@workspace.local"), eq("worker1"), eq(Role.WORKER));
    }

    @Test
    @DisplayName("acceptInvite should reject duplicate username")
    void acceptInvite_duplicateUsername_throwsException() {
        String rawToken = "raw-invite-token";
        String tokenHash = AuthService.hashToken(rawToken);

        Invite invite = Invite.builder()
                .id(UUID.randomUUID())
                .email("worker@workspace.local")
                .role(Role.WORKER)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                .revoked(false)
                .build();

        when(inviteRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(invite));
        when(userRepository.findByEmail("worker@workspace.local")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("alreadytaken")).thenReturn(true);

        AcceptInviteRequest request = AcceptInviteRequest.builder()
                .token(rawToken)
                .username("alreadytaken")
                .password("secretPass")
                .build();

        assertThrows(UsernameAlreadyExistsException.class, () -> authService.acceptInvite(request, "127.0.0.1"));
    }

    @Test
    @DisplayName("verifyInvite should return InviteResponse for valid invite token")
    void verifyInvite_success() {
        String rawToken = "raw-invite-token";
        String tokenHash = AuthService.hashToken(rawToken);

        Invite invite = Invite.builder()
                .id(UUID.randomUUID())
                .email("worker@workspace.local")
                .fullname("New Worker")
                .role(Role.WORKER)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                .revoked(false)
                .build();

        when(inviteRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(invite));

        InviteResponse response = authService.verifyInvite(rawToken);

        assertNotNull(response);
        assertEquals("worker@workspace.local", response.email());
        assertEquals(Role.WORKER, response.role());
        assertTrue(response.valid());
    }

    @Test
    @DisplayName("refresh should rotate token and return new token pair")
    void refresh_success() {
        String rawRefreshToken = "old-refresh-token";
        when(refreshTokenService.rotate(rawRefreshToken, "127.0.0.1")).thenReturn(
                new RefreshTokenService.RotationResult("new-refresh-token", activeAdmin)
        );
        when(jwtService.generateAccessToken(activeAdmin)).thenReturn("new-access-jwt");

        TokenPair result = authService.refresh(rawRefreshToken, "127.0.0.1");

        assertNotNull(result);
        assertEquals("new-access-jwt", result.accessToken());
        assertEquals("new-refresh-token", result.refreshToken());
    }

    @Test
    @DisplayName("changePassword should update password and revoke all refresh tokens")
    void changePassword_success() {
        ChangePasswordRequest request = new ChangePasswordRequest("currentPass", "newPass123", "newPass123");

        when(userRepository.findById(activeAdmin.getId())).thenReturn(Optional.of(activeAdmin));
        when(passwordEncoder.matches("currentPass", activeAdmin.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("newPass123", activeAdmin.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("newPass123")).thenReturn("encodedNewPass");

        authService.changePassword(activeAdmin, request);

        assertEquals("encodedNewPass", activeAdmin.getPassword());
        verify(userRepository).save(activeAdmin);
        verify(refreshTokenService).revokeAllForUser(activeAdmin);
    }

    @Test
    @DisplayName("forgotPassword should generate reset token, persist, and dispatch email")
    void forgotPassword_success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("admin@workspace.local");
        when(userRepository.findByUsernameOrEmail("admin@workspace.local")).thenReturn(Optional.of(activeAdmin));
        when(passwordResetTokenRepository.findByUserAndRevokedFalse(activeAdmin)).thenReturn(Collections.emptyList());

        authService.forgotPassword(request);

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq(activeAdmin.getEmail()), eq(activeAdmin.getFullname()), anyString());
    }

    @Test
    @DisplayName("resetPassword should update password, mark token used, and revoke sessions")
    void resetPassword_success() {
        String rawToken = "raw-reset-token";
        String tokenHash = AuthService.hashToken(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(activeAdmin)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .revoked(false)
                .build();

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.matches("brandNewPass", activeAdmin.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("brandNewPass")).thenReturn("encodedBrandNewPass");
        when(passwordResetTokenRepository.findByUserAndRevokedFalse(activeAdmin)).thenReturn(Collections.singletonList(resetToken));

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token(rawToken)
                .newPassword("brandNewPass")
                .build();

        authService.resetPassword(request);

        assertEquals("encodedBrandNewPass", activeAdmin.getPassword());
        assertNotNull(resetToken.getUsedAt());
        verify(userRepository).save(activeAdmin);
        verify(refreshTokenService).revokeAllForUser(activeAdmin);
    }
}
