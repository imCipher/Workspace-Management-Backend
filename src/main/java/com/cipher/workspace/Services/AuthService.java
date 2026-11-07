package com.cipher.workspace.Services;

import com.cipher.workspace.Config.EmailProperties;
import com.cipher.workspace.DTOs.*;
import com.cipher.workspace.Entity.*;
import com.cipher.workspace.Exception.BadRequestException;
import com.cipher.workspace.Exception.InvalidTokenException;
import com.cipher.workspace.Exception.ResourceNotFoundException;
import com.cipher.workspace.Exception.UsernameAlreadyExistsException;
import com.cipher.workspace.Repository.*;
import com.cipher.workspace.Security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final InviteRepository inviteRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final EmailProperties emailProperties;

    public AuthService(
            UserRepository userRepository,
            InviteRepository inviteRepository,
            RefreshTokenService refreshTokenService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            EmailService emailService,
            EmailProperties emailProperties
    ) {
        this.userRepository = userRepository;
        this.inviteRepository = inviteRepository;
        this.refreshTokenService = refreshTokenService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.emailProperties = emailProperties;
    }

    /**
     * Authenticates a user with username/email and password, issuing access & refresh tokens.
     */
    public AuthResult signIn(SignInRequest request, String clientIp) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            throw new BadRequestException("Username and password are required");
        }

        String identifier = request.getUsername().trim();

        User user = userRepository.findByUsernameOrEmail(identifier)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword())
        );

        User principal = (User) authentication.getPrincipal();

        if (principal.getStatus() == UserStatus.PENDING_INVITE) {
            throw new DisabledException("Your invitation is pending. Please complete account setup using the invite link sent to your email.");
        }

        if (principal.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("Account has been deactivated. Please contact an administrator.");
        }

        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = refreshTokenService.createSession(principal, clientIp);

        return new AuthResult(accessToken, refreshToken, UserResponse.from(principal));
    }

    /**
     * Worker/Admin accepts an invite token, sets their unique username and password, and signs in.
     */
    @Transactional
    public AuthResult acceptInvite(AcceptInviteRequest request, String clientIp) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            throw new InvalidTokenException("Missing invite token");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BadRequestException("Username is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("Password is required");
        }

        String tokenHash = hashToken(request.getToken());
        Invite invite = inviteRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid invite token"));

        if (!invite.isValid()) {
            throw new InvalidTokenException("Invite token is invalid or has expired");
        }

        String username = request.getUsername().trim();

        // Ensure username is not already claimed by another user
        User existingUser = userRepository.findByEmail(invite.getEmail()).orElse(null);

        if (existingUser != null) {
            if (userRepository.existsByUsernameAndIdNot(username, existingUser.getId())) {
                throw new UsernameAlreadyExistsException("Username is already taken. Please choose another.");
            }
        } else {
            if (userRepository.existsByUsername(username)) {
                throw new UsernameAlreadyExistsException("Username is already taken. Please choose another.");
            }
        }

        String fullname = request.getFullname() != null && !request.getFullname().isBlank()
                ? request.getFullname().trim()
                : (invite.getFullname() != null ? invite.getFullname() : invite.getEmail());

        User user = existingUser != null ? existingUser : new User();
        user.setFullname(fullname);
        user.setEmail(invite.getEmail());
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(invite.getRole() != null ? invite.getRole() : Role.WORKER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setInvitedBy(invite.getInvitedBy());

        User savedUser = userRepository.save(user);

        invite.setUsedAt(Instant.now());
        inviteRepository.save(invite);

        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getUsername(), savedUser.getRole());

        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = refreshTokenService.createSession(savedUser, clientIp);

        return new AuthResult(accessToken, refreshToken, UserResponse.from(savedUser));
    }

    /**
     * Validates an invite token and returns details so the frontend can display the setup form.
     */
    @Transactional(readOnly = true)
    public InviteResponse verifyInvite(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidTokenException("Missing invite token");
        }

        String tokenHash = hashToken(rawToken);
        Invite invite = inviteRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid invite token"));

        if (!invite.isValid()) {
            throw new InvalidTokenException("Invite token is invalid or has expired");
        }

        return InviteResponse.from(invite);
    }

    /**
     * Rotates refresh token and returns a new access & refresh token pair.
     */
    public TokenPair refresh(String rawRefreshToken, String currentIp) {
        RefreshTokenService.RotationResult result = refreshTokenService.rotate(rawRefreshToken, currentIp);
        String newAccessToken = jwtService.generateAccessToken(result.user());
        return new TokenPair(newAccessToken, result.refreshToken());
    }

    /**
     * Terminates single refresh token session.
     */
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revokeByToken(rawRefreshToken);
        }
    }

    /**
     * Terminates all active sessions for the user associated with the given refresh token.
     */
    public void logoutAll(String rawRefreshToken) {
        RefreshToken token = refreshTokenService.verify(rawRefreshToken);
        refreshTokenService.revokeAllForUser(token.getUser());
    }

    /**
     * Changes password for an authenticated user and revokes all active sessions.
     */
    @Transactional
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        if (currentUser == null) {
            throw new ResourceNotFoundException("User not found");
        }
        if (request == null || request.getCurrentPassword() == null || request.getNewPassword() == null) {
            throw new BadRequestException("Current password and new password are required");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password cannot be the same as current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Terminate all user sessions so re-login is required with new credentials
        refreshTokenService.revokeAllForUser(user);
    }

    /**
     * Initiates password recovery by emailing a single-use 1-hour reset token.
     */
    @Transactional
    public void forgotPassword(String email) {
        forgotPassword(new ForgotPasswordRequest(email));
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }

        String identifier = request.getEmail().trim();
        User user = userRepository.findByUsernameOrEmail(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with identifier: " + identifier));

        // Invalidate active reset tokens
        List<PasswordResetToken> existingTokens = passwordResetTokenRepository.findByUserAndRevokedFalse(user);
        for (PasswordResetToken token : existingTokens) {
            token.setRevoked(true);
        }
        passwordResetTokenRepository.saveAll(existingTokens);

        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .revoked(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullname(), rawToken);
    }

    /**
     * Validates a password reset token.
     */
    @Transactional(readOnly = true)
    public PasswordResetToken verifyResetToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidTokenException("Missing password reset token");
        }

        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        if (resetToken.getUsedAt() != null) {
            throw new InvalidTokenException("Password reset token has already been used");
        }
        if (resetToken.isRevoked()) {
            throw new InvalidTokenException("Password reset token has been revoked");
        }
        if (resetToken.isExpired()) {
            throw new InvalidTokenException("Password reset token has expired");
        }

        return resetToken;
    }

    /**
     * Resets password using a validated reset token and revokes all active sessions.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            throw new InvalidTokenException("Missing password reset token");
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new BadRequestException("New password is required");
        }

        PasswordResetToken resetToken = verifyResetToken(request.getToken());
        User user = resetToken.getUser();

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password cannot be the same as the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        // Revoke other reset tokens
        List<PasswordResetToken> activeTokens = passwordResetTokenRepository.findByUserAndRevokedFalse(user);
        for (PasswordResetToken token : activeTokens) {
            if (!token.getId().equals(resetToken.getId())) {
                token.setRevoked(true);
            }
        }
        passwordResetTokenRepository.saveAll(activeTokens);

        refreshTokenService.revokeAllForUser(user);
    }

    public UserResponse getCurrentUser(User user) {
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
        User freshUser = user.getId() != null
                ? userRepository.findById(user.getId()).orElse(user)
                : user;
        return UserResponse.from(freshUser);
    }

    @Transactional
    public void deleteAccount(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new ResourceNotFoundException("User not found");
        }
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        refreshTokenService.revokeAllForUser(user);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private static String generateSecureToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }
}
