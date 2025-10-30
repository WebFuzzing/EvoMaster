package com.foo.spring.rest.h2;

import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.WebRequest;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import static springfox.documentation.builders.PathSelectors.regex;

public class SwaggerConfiguration {

    @Bean
    public Docket docketApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .paths(regex("/api/.*"))
                .build()
                .ignoredParameterTypes(WebRequest.class, Authentication.class);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("API")
                .description("Some description")
                .version("1.0")
                .build();
    }
}
