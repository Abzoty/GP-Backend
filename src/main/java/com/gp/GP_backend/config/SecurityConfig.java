package com.gp.GP_backend.config;

import com.gp.GP_backend.security.JwtAuthFilter;
import com.gp.GP_backend.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Central Spring Security configuration.
 *
 * <h3>Design decisions</h3>
 * <ul>
 * <li><b>Stateless sessions</b> — no HTTP session is created; every request
 * must carry a JWT.</li>
 * <li><b>CSRF disabled</b> — safe for stateless REST APIs that do not use
 * cookie-based auth.</li>
 * <li><b>CORS</b> — delegated to the {@link CorsConfig} bean (picked up via
 * {@code Customizer.withDefaults()}).</li>
 * <li><b>Public routes</b> — auth endpoints and Swagger UI are open; everything
 * else requires a valid JWT.</li>
 * <li><b>BCrypt</b> — industry-standard adaptive hash with a default cost
 * factor of 10.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Defines the HTTP security filter chain.
     *
     * <p>
     * Filter execution order (relevant filters only):
     * 
     * <pre>
     *   CorsFilter → JwtAuthFilter → UsernamePasswordAuthenticationFilter → ...
     * </pre>
     * 
     * {@link JwtAuthFilter} runs before Spring Security's default authentication
     * filter so
     * the security context is populated before authorization checks run.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS handled by the CorsFilter bean from CorsConfig
                .cors(Customizer.withDefaults())

                // CSRF not needed for stateless REST APIs
                .csrf(AbstractHttpConfigurer::disable)

                // No server-side sessions — each request is authenticated independently via JWT
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // ── Public routes ────────────────────────────────────────────
                        .requestMatchers(
                                "/api/v1/auth/**", // registration, login, refresh, logout
                                "/swagger-ui/**", // Swagger UI static resources
                                "/swagger-ui.html",
                                "/v3/api-docs/**", // OpenAPI spec (JSON/YAML)
                                "/v3/api-docs.yaml",
                                "/scalar/**" // Scalar UI (Springdoc 3.x)
                        ).permitAll()
                        // ── Everything else requires authentication ──────────────────
                        .anyRequest().authenticated())

                // Wire up the DAO provider (BCrypt + UserDetailsService)
                .authenticationProvider(authenticationProvider())

                // Insert JWT validation before the standard form-login filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Connects Spring Security's authentication mechanism to the database-backed
     * {@link UserDetailsServiceImpl} with BCrypt password verification.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} as a bean so it can be injected
     * into
     * {@link com.gp.GP_backend.domain.user.controller.AuthController} for
     * programmatic login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt password encoder with the default cost factor (10 rounds).
     * Increasing the cost factor improves brute-force resistance at the expense of
     * CPU time.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}