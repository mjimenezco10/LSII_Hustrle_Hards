package com.tecnocampus.LS2.protube_back.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation: OpenAPI description of the REST API, served at /v3/api-docs and browsable at /swagger-ui.html.
 * Every operation declares the JWT bearer scheme, so "Authorize" in Swagger UI sends the token of /api/auth/login;
 * public endpoints simply ignore it.
 */
@Configuration
public class OpenApiConfig {

    static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI protubeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ProTube API")
                        .description("REST API of ProTube: videos, comments, ratings, favourites, subscriptions, "
                                + "analytics and accounts. Log in with POST /api/auth/login and paste the token "
                                + "in \"Authorize\" to call the endpoints that need a user.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
