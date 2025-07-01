package com.searchengine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration pour la documentation OpenAPI avec Swagger.
 */
@Configuration
public class SwaggerConfig {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerConfig.class);

    /**
     * Configure la documentation OpenAPI pour l'API du moteur de recherche.
     *
     * @return Configuration OpenAPI
     */
    @Bean
    public OpenAPI customOpenAPI() {
        logger.info("Configuration de la documentation OpenAPI");
        return new OpenAPI()
                .info(new Info()
                        .title("Intelligent Search Engine API")
                        .version("1.0.0")
                        .description("API pour l'indexation et la recherche de données à partir d'agents de crawling")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }

    /**
     * Configure un groupe d'API pour les endpoints d'indexation.
     *
     * @return GroupedOpenApi pour l'API d'indexation
     */
    @Bean
    public GroupedOpenApi indexingApi() {
        return GroupedOpenApi.builder()
                .group("indexing")
                .pathsToMatch("/api/index/**")
                .build();
    }

    /**
     * Configure un groupe d'API pour les endpoints de vérification de santé.
     *
     * @return GroupedOpenApi pour l'API de santé
     */
    @Bean
    public GroupedOpenApi healthApi() {
        return GroupedOpenApi.builder()
                .group("health")
                .pathsToMatch("/api/health/**")
                .build();
    }
}