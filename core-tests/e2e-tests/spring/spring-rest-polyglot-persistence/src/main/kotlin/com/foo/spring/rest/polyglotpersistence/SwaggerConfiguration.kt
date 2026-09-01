package com.foo.spring.rest.polyglotpersistence

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.GroupedOpenApi
import org.springframework.context.annotation.Bean

open class SwaggerConfiguration {

    @Bean
    open fun api(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("api")
            .pathsToMatch("/api/**")
            .build()
    }

    @Bean
    open fun openApi(): OpenAPI {
        return OpenAPI().info(
            Info()
                .title("Polyglot Persistence API")
                .description("API with endpoints reading simultaneously Mongo, Redis and Postgres databases")
                .version("1.0")
        )
    }
}
