package com.cipher.workspace.Security;

import com.cipher.workspace.Entity.Role;
import com.cipher.workspace.Entity.User;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should pass request without Authorization header to next filter unchanged")
    void doFilter_withoutAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("Should authenticate request when valid Bearer token is provided")
    void doFilter_withValidToken_setsSecurityContext() throws Exception {
        String token = "valid.jwt.token";
        String username = "worker1";
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .role(Role.WORKER)
                .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(user);
        when(jwtService.isValid(token, user)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_WORKER")));
    }

    @Test
    @DisplayName("Should clear context when token validation fails")
    void doFilter_withInvalidToken_clearsSecurityContext() throws Exception {
        String token = "invalid.jwt.token";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername(token)).thenThrow(new RuntimeException("Expired or tampered token"));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
