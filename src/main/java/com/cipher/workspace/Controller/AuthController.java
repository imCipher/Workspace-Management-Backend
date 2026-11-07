package com.cipher.workspace.Controller;

import com.cipher.workspace.DTOs.*;
import com.cipher.workspace.Entity.Invite;
import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Exception.InvalidTokenException;
import com.cipher.workspace.Security.RefreshTokenCookieHandler;
import com.cipher.workspace.Services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Authentication", description = "Endpoints for user authentication, invite acceptance, password reset, and session management")
@RestController
@RequestMapping({"/api/auth", "/auth"})
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieHandler cookieHandler;

    public AuthController(AuthService authService, RefreshTokenCookieHandler cookieHandler) {
        this.authService = authService;
        this.cookieHandler = cookieHandler;
    }

    @Operation(summary = "User sign in", description = "Authenticates user with username or email, returns JWT access token and sets HttpOnly refreshToken cookie.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Account disabled or pending invite")
    })
    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signIn(
            @Valid @RequestBody SignInRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = resolveClientIp(httpRequest);
        AuthResult result = authService.signIn(request, clientIp);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieHandler.createRefreshCookie(result.refreshToken()).toString())
                .body(new AuthResponse(result.accessToken(), "Bearer", result.user()));
    }

    @Operation(summary = "Verify invite token", description = "Validates an invitation token for user onboarding before displaying registration form.")
    @GetMapping("/verify-invite")
    public ResponseEntity<Map<String, Object>> verifyInvite(@RequestParam("token") String token) {
        InviteResponse invite = authService.verifyInvite(token);
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "email", invite.email(),
                "role", invite.role() != null ? invite.role().name() : "WORKER"
        ));
    }

    @Operation(summary = "Accept team invitation", description = "Finalizes onboarding: user sets chosen username and password, activates account, and receives access token.")
    @PostMapping("/accept-invite")
    public ResponseEntity<AuthResponse> acceptInvite(
            @Valid @RequestBody AcceptInviteRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = resolveClientIp(httpRequest);
        AuthResult result = authService.acceptInvite(request, clientIp);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookieHandler.createRefreshCookie(result.refreshToken()).toString())
                .body(new AuthResponse(result.accessToken(), "Bearer", result.user()));
    }

    @Operation(summary = "Refresh access token", description = "Rotates refresh token and issues a new access token using the HttpOnly cookie.")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest httpRequest) {
        String refreshToken = cookieHandler.extractRefreshToken(httpRequest)
                .orElseThrow(() -> new InvalidTokenException("Missing refresh token"));

        String clientIp = resolveClientIp(httpRequest);
        TokenPair tokens = authService.refresh(refreshToken, clientIp);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieHandler.createRefreshCookie(tokens.refreshToken()).toString())
                .body(new TokenResponse(tokens.accessToken(), "Bearer"));
    }

    @Operation(summary = "Logout", description = "Revokes server-side refresh token and clears refresh cookie.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        cookieHandler.extractRefreshToken(httpRequest).ifPresent(authService::logout);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieHandler.createClearCookie().toString())
                .build();
    }

    @Operation(summary = "Logout all sessions", description = "Revokes all refresh tokens across all devices for the user.")
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(HttpServletRequest httpRequest) {
        cookieHandler.extractRefreshToken(httpRequest).ifPresent(authService::logoutAll);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieHandler.createClearCookie().toString())
                .build();
    }

    @Operation(summary = "Request password reset", description = "Sends password reset email link with secure 1-hour token.")
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a password reset link has been sent."));
    }

    @Operation(summary = "Verify password reset token", description = "Validates reset token from email before showing password form.")
    @GetMapping("/verify-reset-token")
    public ResponseEntity<Map<String, Object>> verifyResetToken(@RequestParam("token") String token) {
        authService.verifyResetToken(token);
        return ResponseEntity.ok(Map.of("valid", true, "message", "Token is valid"));
    }

    @Operation(summary = "Reset password", description = "Resets user password with valid reset token.")
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully. Please log in with your new password."));
    }

    @Operation(summary = "Get current authenticated user profile", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.getCurrentUser(user));
    }

    @Operation(summary = "Change password", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(user, request);
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieHandler.createClearCookie().toString())
                .body(Map.of("message", "Password changed successfully. Please log in again with your new password."));
    }

    @Operation(summary = "Delete user account", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal User user) {
        authService.deleteAccount(user);
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieHandler.createClearCookie().toString())
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
