package com.cipher.workspace.Config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Workspace Management API")
                        .version("1.0.0")
                        .description("RESTful API documentation for the Single-Facility Workspace Management System.\n\n"
                                + "### Overview\n"
                                + "Designed for internal back-office facility operations, covering:\n"
                                + "- **Desk Operations & Attendance:** Visitor check-in, check-out, time log adjustments, and active in-space metrics.\n"
                                + "- **Customer Management & Analytics:** Full customer profiles, visit history, regular frequency tracking based on calendar month.\n"
                                + "- **Financial Management:** Operating expense logging, categorized cost tracking, and Profit & Loss reports.\n"
                                + "- **Team Management:** Invite-based worker onboarding with email verification, staff directory, and access revocation.\n\n"
                                + "### Authentication\n"
                                + "Secured endpoints require a valid JWT Bearer token passed in the `Authorization` header (`Bearer <token>`).\n"
                                + "1. Obtain your access token via `POST /api/auth/signin`.\n"
                                + "2. Click the **Authorize** button below and enter your Bearer token.\n"
                                + "3. Role-based access is strictly divided into `ROLE_ADMIN` and `ROLE_WORKER`.")
                        .contact(new Contact()
                                .name("Facility Operations Support")
                                .email("support@workspace.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("/api").description("Default API context path"),
                        new Server().url("http://localhost:8080/api").description("Local development server")
                ))
                .tags(List.of(
                        new Tag().name("Authentication").description("Endpoints for user authentication, invite acceptance, password reset, and session controls"),
                        new Tag().name("Visits & Check-in Desk").description("Endpoints for visitor check-in, check-out, time log editing, visit history, and real-time desk metrics (Worker or Admin)"),
                        new Tag().name("Customer Management").description("Endpoints for viewing customer profiles and detailed visit histories (Worker or Admin)"),
                        new Tag().name("Customer Analytics").description("Endpoints for analyzing customer visits, frequency metrics, and regular customer trends (Admin only)"),
                        new Tag().name("Expenses Management").description("Endpoints for logging operational expenses, managing expense categories, and tracking costs (Admin only)"),
                        new Tag().name("Financial Reports").description("Endpoints for generating financial statements and profit/loss summaries (Admin only)"),
                        new Tag().name("Team Management").description("Endpoints for managing workspace workers and invitations (Admin only)"),
                        new Tag().name("Workspace Options & Metadata").description("Endpoints for retrieving dynamic service types, payment types, and expense category options"),
                        new Tag().name("Health").description("System health and liveness checks (Public)")
                ))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                                .name(BEARER_AUTH_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT access token (without the 'Bearer ' prefix).")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
    }
}
