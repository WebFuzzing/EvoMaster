package com.opensearch.config;

import static springfox.documentation.builders.PathSelectors.regex;

import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.WebRequest;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

public class SwaggerConfiguration {

    private final String path;

    public SwaggerConfiguration(String path) {
        this.path = path;
    }

    @Bean
    public Docket docketApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .paths(regex("/" + path + "/.*"))
                .build()
                .ignoredParameterTypes(WebRequest.class, Authentication.class);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("OpenSearch Sample API")
                .description("OpenSearch Sample API description for " + path)
                .version("1.0")
                .build();
    }
}
