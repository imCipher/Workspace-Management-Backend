package com.cipher.workspace.Config;

import com.cipher.workspace.Security.CustomUserDetailsService;
import com.cipher.workspace.Security.JwtAccessDeniedHandler;
import com.cipher.workspace.Security.JwtAuthenticationEntryPoint;
import com.cipher.workspace.Security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            JwtAuthenticationEntryPoint authenticationEntryPoint,
            JwtAccessDeniedHandler accessDeniedHandler,
            CorsConfigurationSource corsConfigurationSource
    ) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
                // Browser SPA CORS support
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // Stateless Bearer JWT API with HttpOnly SameSite cookies -> CSRF disabled
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public system endpoints & documentation
                        .requestMatchers(
                                "/health", "/api/health",
                                "/v3/api-docs", "/v3/api-docs/**",
                                "/api/v3/api-docs", "/api/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html",
                                "/api/swagger-ui/**", "/api/swagger-ui.html",
                                "/swagger-resources", "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // Public authentication endpoints (Invite-only onboarding & password recovery)
                        .requestMatchers(
                                "/auth/signin", "/api/auth/signin",
                                "/auth/refresh", "/api/auth/refresh",
                                "/auth/accept-invite", "/api/auth/accept-invite",
                                "/auth/verify-invite", "/api/auth/verify-invite",
                                "/auth/forgot-password", "/api/auth/forgot-password",
                                "/auth/reset-password", "/api/auth/reset-password"
                        ).permitAll()

                        // Authenticated staff profile & session controls
                        .requestMatchers(
                                "/auth/me", "/api/auth/me",
                                "/auth/change-password", "/api/auth/change-password",
                                "/auth/logout", "/api/auth/logout",
                                "/auth/logout-all", "/api/auth/logout-all",
                                "/auth/account", "/api/auth/account"
                        ).authenticated()

                        // Strict Admin-only endpoints: Workers receive 403 Forbidden
                        .requestMatchers(
                                "/team", "/team/**", "/api/team", "/api/team/**",
                                "/admin", "/admin/**", "/api/admin", "/api/admin/**",
                                "/expenses", "/expenses/**", "/api/expenses", "/api/expenses/**",
                                "/reports", "/reports/**", "/api/reports", "/api/reports/**",
                                "/analytics", "/analytics/**", "/api/analytics", "/api/analytics/**"
                        ).hasRole("ADMIN")

                        // Option/Category mutations: Admin-only
                        .requestMatchers(HttpMethod.POST, "/options/**", "/categories/**", "/api/options/**", "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/options/**", "/categories/**", "/api/options/**", "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/options/**", "/categories/**", "/api/options/**", "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/options/**", "/categories/**", "/api/options/**", "/api/categories/**").hasRole("ADMIN")

                        // Desk operations, Customer directory, and Option lookups: accessible to WORKER and ADMIN
                        .requestMatchers(
                                "/visits", "/visits/**", "/api/visits", "/api/visits/**",
                                "/customers", "/customers/**", "/api/customers", "/api/customers/**",
                                "/options/**", "/categories/**", "/api/options/**", "/api/categories/**"
                        ).hasAnyRole("WORKER", "ADMIN")

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
