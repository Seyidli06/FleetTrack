package com.fleettrack.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    public static final String BEARER_AUTH =
            "bearerAuth";

    @Bean
    public OpenAPI fleetTrackOpenApi() {

        SecurityScheme securityScheme =
                new SecurityScheme()
                        .name(BEARER_AUTH)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT");

        Components components =
                new Components()
                        .addSecuritySchemes(
                                BEARER_AUTH,
                                securityScheme
                        );

        Info info =
                new Info()
                        .title("FleetTrack API")
                        .description(
                                """
                                Fleet management backend API.

                                Features include vehicle and driver management,
                                vehicle assignments, maintenance lifecycle,
                                real-time GPS tracking, Redis caching,
                                scheduling and JWT-based security.
                                """
                        )
                        .version("v1")
                        .contact(
                                new Contact()
                                        .name("FleetTrack")
                        )
                        .license(
                                new License()
                                        .name("Private Project")
                        );

        return new OpenAPI()
                .info(info)
                .components(components);
    }
}