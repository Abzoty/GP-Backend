package com.gp.GP_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Swagger / OpenAPI documentation (available at
 * {@code /swagger-ui/index.html}).
 *
 * <h3>JWT in Swagger UI</h3>
 * The {@code bearerAuth} security scheme adds an "Authorize" button to Swagger
 * UI.
 * After logging in via {@code POST /api/v1/auth/login}, paste the returned
 * {@code token}
 * value (WITHOUT the "Bearer " prefix — Swagger adds it automatically) into the
 * Authorize dialog. All subsequent requests will include the
 * {@code Authorization: Bearer}
 * header automatically.
 */
@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI customOpenAPI() {
                final String securitySchemeName = "bearerAuth";

                return new OpenAPI()
                                // Apply JWT auth globally — every endpoint shows the lock icon
                                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                                .components(new Components()
                                                .addSecuritySchemes(securitySchemeName,
                                                                new SecurityScheme()
                                                                                .name(securitySchemeName)
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")
                                                                                .description("Paste the access token from POST /api/v1/auth/login")))
                                .info(new Info()
                                                .title("Covalent — GP Backend API")
                                                .version("2.0")
                                                .description("REST API for the Covalent academic community platform"));
        }

        /**
         * Groups all API endpoints under a single "public-apis" group in Swagger UI.
         * Add more groups here if you want to split the documentation by domain.
         */
        @Bean
        public GroupedOpenApi publicApi() {
                return GroupedOpenApi.builder()
                                .group("public-apis")
                                .pathsToMatch("/**")
                                .build();
        }
}