package com.cipher.workspace.Controller;

import com.cipher.workspace.DTOs.*;
import com.cipher.workspace.Entity.PasswordResetToken;
import com.cipher.workspace.Entity.Role;
import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Entity.UserStatus;
import com.cipher.workspace.Exception.GlobalExceptionHandler;
import com.cipher.workspace.Security.RefreshTokenCookieHandler;
import com.cipher.workspace.Services.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private RefreshTokenCookieHandler cookieHandler;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void signIn_success_returns200AndSetsCookie() throws Exception {
        SignInRequest request = new SignInRequest("admin", "Admin@12345");
        UserResponse userResponse = new UserResponse(
                UUID.randomUUID(), "Admin User", "admin", "admin@workspace.com", Role.ADMIN, UserStatus.ACTIVE, null, true, LocalDateTime.now()
        );
        AuthResult authResult = new AuthResult("access-token-123", "refresh-token-456", userResponse);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "refresh-token-456").httpOnly(true).path("/api/auth").build();

        when(authService.signIn(any(), any())).thenReturn(authResult);
        when(cookieHandler.createRefreshCookie("refresh-token-456")).thenReturn(cookie);

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("admin"));
    }

    @Test
    void signIn_badCredentials_returns401() throws Exception {
        SignInRequest request = new SignInRequest("admin", "wrong-password");

        when(authService.signIn(any(), any())).thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void verifyInvite_returns200() throws Exception {
        InviteResponse invite = new InviteResponse(
                UUID.randomUUID(), "worker@workspace.com", "Worker User", Role.WORKER, "admin", Instant.now().plusSeconds(3600), true
        );
        when(authService.verifyInvite("token-123")).thenReturn(invite);

        mockMvc.perform(get("/api/auth/verify-invite?token=token-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value("worker@workspace.com"))
                .andExpect(jsonPath("$.role").value("WORKER"));
    }

    @Test
    void acceptInvite_returns201() throws Exception {
        AcceptInviteRequest request = new AcceptInviteRequest("token-xyz", "newworker", "Secret@123", "New Worker");
        UserResponse userResponse = new UserResponse(
                UUID.randomUUID(), "New Worker", "newworker", "worker@workspace.com", Role.WORKER, UserStatus.ACTIVE, null, true, LocalDateTime.now()
        );
        AuthResult authResult = new AuthResult("access-xyz", "refresh-xyz", userResponse);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "refresh-xyz").build();

        when(authService.acceptInvite(any(), any())).thenReturn(authResult);
        when(cookieHandler.createRefreshCookie("refresh-xyz")).thenReturn(cookie);

        mockMvc.perform(post("/api/auth/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-xyz"))
                .andExpect(jsonPath("$.user.username").value("newworker"));
    }

    @Test
    void refresh_returns200() throws Exception {
        when(cookieHandler.extractRefreshToken(any())).thenReturn(Optional.of("valid-refresh-token"));
        TokenPair tokens = new TokenPair("new-access-token", "new-refresh-token");
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "new-refresh-token").build();

        when(authService.refresh(eq("valid-refresh-token"), any())).thenReturn(tokens);
        when(cookieHandler.createRefreshCookie("new-refresh-token")).thenReturn(cookie);

        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    void logout_returns204() throws Exception {
        when(cookieHandler.extractRefreshToken(any())).thenReturn(Optional.of("token-to-revoke"));
        doNothing().when(authService).logout("token-to-revoke");
        when(cookieHandler.createClearCookie()).thenReturn(ResponseCookie.from("refreshToken", "").maxAge(0).build());

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    void forgotPassword_returns200() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@workspace.com");
        doNothing().when(authService).forgotPassword(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void verifyResetToken_returns200() throws Exception {
        PasswordResetToken resetToken = new PasswordResetToken();
        when(authService.verifyResetToken("reset-xyz")).thenReturn(resetToken);

        mockMvc.perform(get("/api/auth/verify-reset-token?token=reset-xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void resetPassword_returns200() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("reset-xyz", "NewSecret@123", "NewSecret@123");
        doNothing().when(authService).resetPassword(any());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
