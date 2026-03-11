package com.gp.GP_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * HTTP client configuration for calling external services.
 *
 * <p>
 * Uses Spring's {@link RestClient} (introduced in Spring 6.1 / Spring Boot 3.2)
 * which provides a fluent, synchronous API without requiring WebFlux on the
 * classpath.
 * It uses Apache HttpClient 5 under the hood (already in {@code pom.xml}).
 *
 * <p>
 * The ML service base URL is configured in {@code application-dev.properties}
 * and defaults to {@code http://localhost:5001} if the property is absent.
 */
@Configuration
public class WebClientConfig {

    @Value("${ml.service.base-url:http://localhost:5001}")
    private String mlServiceBaseUrl;

    /**
     * Pre-configured REST client for the Python ML recommendation service.
     *
     * <p>
     * Inject this bean into recommendation services:
     * 
     * <pre>{@code
     * private final RestClient mlRestClient;
     * }</pre>
     */
    @Bean
    public RestClient mlRestClient() {
        return RestClient.builder()
                .baseUrl(mlServiceBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
