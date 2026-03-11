package com.gp.GP_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the OpenAPI (Swagger) documentation available at:
 * <ul>
 * <li>Swagger UI: {@code /swagger-ui/index.html}</li>
 * <li>OpenAPI spec: {@code /v3/api-docs}</li>
 * </ul>
 *
 * <p>
 * The Bearer token scheme is applied globally so that the "Authorize" button
 * in Swagger UI pre-fills the {@code Authorization: Bearer <token>} header
 * for every request. Copy a token from the login response and paste it there.
 */
@Configuration
public class OpenApiConfig {

        private static final String SECURITY_SCHEME_NAME = "bearerAuth";

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                // Apply the bearer auth scheme to all endpoints by default
                                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                                .components(new Components()
                                                .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                                                new SecurityScheme()
                                                                                .name(SECURITY_SCHEME_NAME)
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")
                                                                                .description("Paste your JWT access token here (without 'Bearer ' prefix)")))
                                .info(new Info()
                                                .title("Covalent GP Backend API")
                                                .description("REST API documentation for the Covalent graduation project platform")
                                                .version("1.0")
                                                .contact(new Contact().name("Covalent Team")));
        }

        /**
         * Groups all API endpoints under one group for the Swagger UI dropdown.
         * Add more groups here if you want to separate public vs. private APIs.
         */
        @Bean
        public GroupedOpenApi publicApi() {
                return GroupedOpenApi.builder()
                                .group("all-endpoints")
                                .pathsToMatch("/**")
                                .build();
        }
}
