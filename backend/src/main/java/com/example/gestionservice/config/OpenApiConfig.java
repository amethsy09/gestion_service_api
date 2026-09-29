package com.example.gestionservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gestion Service API")
                        .description("""
                                **Microservice de gestion de services et prestations.**
                                
                                ## Authentification
                                Le JWT est émis par le **Wallet externe**.
                                Ajouter le header : `Authorization: Bearer <JWT_TOKEN>`
                                
                                ## Rôles
                                - `ROLE_USER` — créer et payer ses demandes
                                - `ROLE_ADMIN` — gérer le catalogue, les ressources, les responsables
                                - `ROLE_RESPONSIBLE` — gérer les prestations et les tâches
                                
                                ## Note Paiement
                                Le PIN n'est jamais stocké ni loggé.
                                Les paiements utilisent une `idempotencyKey` pour éviter les doubles débits.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Équipe Gestion Service")
                                .email("dev@gestion-service.sn"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentation complète")
                        .url("https://github.com/gestion-service"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT émis par le Wallet — format : Bearer {token}")));
    }
}
