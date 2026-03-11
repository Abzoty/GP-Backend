package com.gp.GP_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS (Cross-Origin Resource Sharing) configuration.
 *
 * <p>
 * Allows the React frontend (Vite dev server) and the Swagger UI
 * to make authenticated requests to this backend.
 *
 * <p>
 * The {@link CorsFilter} bean is picked up automatically by
 * {@link SecurityConfig} via {@code .cors(Customizer.withDefaults())},
 * which looks for a bean of type
 * {@link org.springframework.web.cors.CorsConfigurationSource}
 * or a {@link CorsFilter} bean in the context.
 *
 * <p>
 * <b>Production note:</b> Replace the allowed origins list with your actual
 * deployed frontend URL. Never use {@code "*"} with
 * {@code allowCredentials(true)}.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Explicitly list allowed origins — wildcards cannot be used with credentials
        config.setAllowedOrigins(List.of(
                "http://localhost:5173", // Vite React dev server
                "http://localhost:8080" // Swagger UI (same-origin requests from browser)
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow all headers so that the JWT Authorization header passes through
        config.setAllowedHeaders(List.of("*"));

        // Required to allow the client to send the Authorization header with
        // credentials
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
