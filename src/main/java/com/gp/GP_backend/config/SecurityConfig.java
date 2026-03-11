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
 * Spring Security configuration for the stateless JWT-based API.
 *
 * <p>
 * Key decisions:
 * <ul>
 * <li><b>STATELESS sessions:</b> No HTTP session is created; every request must
 * carry a JWT.</li>
 * <li><b>CSRF disabled:</b> Safe for a stateless API because there are no
 * cookies carrying
 * session state that a CSRF attack could exploit.</li>
 * <li><b>CORS:</b> Delegated to the {@link CorsConfig} bean via
 * {@code Customizer.withDefaults()}.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Use the CorsFilter bean defined in CorsConfig
                .cors(Customizer.withDefaults())

                // No session cookies — pure stateless REST API
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — no JWT required
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/swagger-ui/**", // Swagger UI static assets
                                "/swagger-ui.html",
                                "/v3/api-docs/**", // OpenAPI spec (JSON/YAML)
                                "/v3/api-docs.yaml",
                                "/scalar/**" // Scalar API reference (springdoc 3.x)
                        ).permitAll()
                        // Every other request must have a valid JWT
                        .anyRequest().authenticated())

                .authenticationProvider(authenticationProvider())

                // Run our JWT filter before Spring's default username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Wires our {@link UserDetailsServiceImpl} and BCrypt password encoder into
     * the DAO-based authentication provider used by {@link AuthenticationManager}.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} as a bean so that
     * {@link com.gp.GP_backend.domain.user.controller.AuthController} can inject it
     * to manually authenticate login requests.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt password encoder with default strength (10 rounds).
     * Increasing the strength improves security at the cost of hashing speed.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
