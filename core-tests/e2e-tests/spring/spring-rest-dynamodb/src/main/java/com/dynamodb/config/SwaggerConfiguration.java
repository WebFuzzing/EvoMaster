package com.dynamodb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.WebRequest;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import static springfox.documentation.builders.PathSelectors.regex;

/**
 * Configures Swagger for the DynamoDB E2E application.
 */
public class SwaggerConfiguration {

    /**
     * Creates the Swagger docket exposed by the application.
     *
     * @return the Swagger docket
     */
    @Bean
    public Docket docketApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .paths(regex("/players/.*"))
                .build()
                .ignoredParameterTypes(WebRequest.class, Authentication.class);
    }

    /**
     * Creates metadata for the generated Swagger schema.
     *
     * @return the API metadata
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("World Cup Players API")
                .description("DynamoDB query sample for World Cup players")
                .version("1.0")
                .build();
    }
}
