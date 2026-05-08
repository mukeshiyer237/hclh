package com.example.demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger UI is available at:  http://localhost:8080/swagger-ui.html
 * OpenAPI JSON spec at:        http://localhost:8080/v3/api-docs
 *
 * All protected endpoints require a Bearer JWT.
 * Click "Authorize" in Swagger UI and paste: Bearer <token>
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Demo API")
                        .version("1.0.0")
                        .description("Spring Boot service with JWT auth, Kafka, and Flyway")
                        .contact(new Contact()
                                .name("Demo Team")
                                .email("demo@example.com")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local"),
                        new Server().url("https://api.example.com").description("Production")
                ))
                // Apply bearerAuth globally — every endpoint requires it by default.
                // Per-endpoint overrides can use @SecurityRequirements(value = {}) to make public.
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your JWT token. Prefix 'Bearer ' is added automatically.")));
    }
}
