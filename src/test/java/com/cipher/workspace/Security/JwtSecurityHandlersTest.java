package com.cipher.workspace.Security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;

class JwtSecurityHandlersTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();
    private final JwtAccessDeniedHandler accessDeniedHandler = new JwtAccessDeniedHandler();

    @Test
    @DisplayName("JwtAuthenticationEntryPoint should return 401 JSON with proper schema")
    void commence_returns401Json() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/admin/metrics");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Bad token"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());

        JsonNode json = mapper.readTree(response.getContentAsString());
        assertEquals(401, json.get("status").asInt());
        assertEquals("Unauthorized", json.get("error").asText());
        assertEquals("/api/admin/metrics", json.get("path").asText());
        assertTrue(json.has("timestamp"));
        assertTrue(json.has("message"));
    }

    @Test
    @DisplayName("JwtAccessDeniedHandler should return 403 JSON with proper schema")
    void handle_returns403Json() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/expenses/create");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("Forbidden"));

        assertEquals(403, response.getStatus());
        assertEquals("application/json", response.getContentType());

        JsonNode json = mapper.readTree(response.getContentAsString());
        assertEquals(403, json.get("status").asInt());
        assertEquals("Forbidden", json.get("error").asText());
        assertEquals("/api/expenses/create", json.get("path").asText());
        assertTrue(json.has("timestamp"));
        assertTrue(json.has("message"));
    }
}
