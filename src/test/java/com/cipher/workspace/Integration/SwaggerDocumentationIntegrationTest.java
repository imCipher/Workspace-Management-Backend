package com.cipher.workspace.Integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SwaggerDocumentationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc getMockMvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("GET /v3/api-docs returns valid OpenAPI JSON specification")
    void apiDocsEndpoint_returnsOkAndValidJson() throws Exception {
        org.springframework.test.web.servlet.MvcResult result = getMockMvc().perform(get("/v3/api-docs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").value(startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("Workspace Management API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andReturn();

        java.nio.file.Files.writeString(
                java.nio.file.Path.of("openapi.json"),
                result.getResponse().getContentAsString()
        );
    }

    @Test
    @DisplayName("OpenAPI specification defines bearerAuth security scheme")
    void apiDocs_definesBearerAuthScheme() throws Exception {
        getMockMvc().perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    @Test
    @DisplayName("OpenAPI specification contains all expected controller tags")
    void apiDocs_containsControllerTags() throws Exception {
        getMockMvc().perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[*].name", hasItems(
                        "Authentication",
                        "Visits & Check-in Desk",
                        "Customer Management",
                        "Customer Analytics",
                        "Expenses Management",
                        "Financial Reports",
                        "Team Management",
                        "Workspace Options & Metadata",
                        "Health"
                )));
    }

    @Test
    @DisplayName("OpenAPI specification documents core endpoints")
    void apiDocs_documentsCoreEndpoints() throws Exception {
        getMockMvc().perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/signin']").exists())
                .andExpect(jsonPath("$.paths['/api/visits/check-in']").exists())
                .andExpect(jsonPath("$.paths['/api/customers']").exists())
                .andExpect(jsonPath("$.paths['/api/expenses']").exists())
                .andExpect(jsonPath("$.paths['/api/reports/financial']").exists())
                .andExpect(jsonPath("$.paths['/api/health']").exists());
    }

    @Test
    @DisplayName("OpenAPI schemas exclude synthetic helper methods like isPasswordConfirmed")
    void apiDocs_excludesSyntheticValidationFields() throws Exception {
        getMockMvc().perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ChangePasswordRequest.properties.passwordConfirmed").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.ChangePasswordRequest.properties.currentPassword").exists())
                .andExpect(jsonPath("$.components.schemas.ChangePasswordRequest.properties.newPassword").exists())
                .andExpect(jsonPath("$.components.schemas.ResetPasswordRequest.properties.passwordConfirmed").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.ResetPasswordRequest.properties.token").exists())
                .andExpect(jsonPath("$.components.schemas.ResetPasswordRequest.properties.newPassword").exists());
    }

    @Test
    @DisplayName("Swagger UI HTML page is accessible")
    void swaggerUi_isAccessible() throws Exception {
        getMockMvc().perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}
