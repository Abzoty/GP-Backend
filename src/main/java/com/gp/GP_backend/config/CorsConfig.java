package com.gp.GP_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Configures Cross-Origin Resource Sharing (CORS) for the application.
 *
 * <p>
 * This bean is picked up automatically by {@link SecurityConfig} via
 * {@code .cors(Customizer.withDefaults())}, which looks for a
 * {@link CorsFilter} bean.
 *
 * <h3>Allowed origins</h3>
 * <ul>
 * <li>{@code http://localhost:5173} — Vite dev server (frontend).</li>
 * <li>{@code http://localhost:8080} — local backend itself (Swagger UI).</li>
 * </ul>
 *
 * <p>
 * For production, replace these origins with the actual deployed frontend URL
 * (e.g. {@code https://covalent.example.com}) — NEVER use {@code *} with
 * credentials.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Explicit list of trusted origins (wildcard not allowed when credentials =
        // true)
        config.setAllowedOrigins(List.of(
                "http://localhost:5173", // Vite frontend dev server
                "http://localhost:8080" // Swagger UI (same host as backend)
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow all request headers — required for the JWT Authorization header
        config.setAllowedHeaders(List.of("*"));

        // Must be true for the browser to include the Authorization header in
        // cross-origin requests
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}