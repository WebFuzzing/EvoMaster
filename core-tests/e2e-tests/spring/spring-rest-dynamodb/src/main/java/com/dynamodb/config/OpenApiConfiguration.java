package com.dynamodb.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the OpenAPI document for the DynamoDB E2E application.
 */
@Configuration
public class OpenApiConfiguration {

    /**
     * Creates metadata for the generated OpenAPI document.
     *
     * @return the OpenAPI document metadata
     */
    @Bean
    public OpenAPI worldCupPlayersOpenApi() {
        return new OpenAPI().info(new Info()
                .title("World Cup Players API")
                .description("DynamoDB query sample for World Cup players")
                .version("1.0"));
    }
}
