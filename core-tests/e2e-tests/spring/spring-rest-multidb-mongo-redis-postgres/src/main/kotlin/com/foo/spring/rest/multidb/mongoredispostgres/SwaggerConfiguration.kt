package com.foo.spring.rest.multidb.mongoredispostgres

import org.springframework.context.annotation.Bean
import org.springframework.security.core.Authentication
import org.springframework.web.context.request.WebRequest
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.service.ApiInfo
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.builders.PathSelectors.regex

open class SwaggerConfiguration {

    @Bean
    open fun docketApi(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
            .apiInfo(apiInfo())
            .select()
            .paths(regex("/api/.*"))
            .build()
            .ignoredParameterTypes(WebRequest::class.java, Authentication::class.java)
    }

    private fun apiInfo(): ApiInfo {
        return ApiInfoBuilder()
            .title("Multi-DB Mongo Redis Postgres API")
            .description("API with endpoints reading Mongo, Redis and Postgres")
            .version("1.0")
            .build()
    }
}
