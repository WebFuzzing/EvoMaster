package com.foo.spring.rest.mysql

import org.springframework.context.annotation.Bean
import org.springframework.security.core.Authentication
import org.springframework.web.context.request.WebRequest
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket

open class SwaggerConfiguration {

    @Bean
    open fun docketApi(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
            .apiInfo(apiInfo())
            .select()
            .paths(PathSelectors.regex("/api/.*"))
            .build()
            .ignoredParameterTypes(WebRequest::class.java, Authentication::class.java)
    }

    private fun apiInfo(): ApiInfo {
        return ApiInfoBuilder()
            .title("API")
            .description("Some description")
            .version("1.0")
            .build()
    }
}