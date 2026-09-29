package com.foo.neo4j;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;

public class OpenApiConfiguration {

    @Bean
    public OpenAPI neo4jOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Neo4j E2E API")
                .description("Neo4j samples used by EvoMaster end-to-end tests")
                .version("1.0"));
    }
}
