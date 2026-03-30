package com.blogplatform.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Blog Platform API")
                        .description("""
                                Production-ready RESTful API for a full-featured Blogging Platform.
                                
                                **Features:**
                                - JWT-based Authentication & Authorization
                                - Role-based Access Control (User / Moderator / Admin)
                                - Post CRUD with categories and tags
                                - Nested comment system
                                - Like / Dislike reactions
                                - Admin dashboard & moderation tools
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Blog Platform Team")
                                .email("support@blogplatform.com"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort + "/api").description("Local"),
                        new Server().url("https://api.blogplatform.com").description("Production")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
