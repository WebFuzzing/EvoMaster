package com.foo.rest.examples.spring.adaptivehypermutation;

import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.WebRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import static springfox.documentation.builders.PathSelectors.regex;
@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class AWHResApp {
  @Bean
  public Docket docketApi() {
    return new Docket(DocumentationType.SWAGGER_2)
        .apiInfo(apiInfo())
        .select()
        .paths(regex("/api/.*"))
        .build()
        .ignoredParameterTypes(WebRequest.class, Authentication.class);
  }

  public ApiInfo apiInfo() {
    return new ApiInfoBuilder()
        .title("Auto API for rest resources")
        .description("description")
        .version("2.0.0")
        .build();
  }
  // http://localhost:8080/v2/api-docs
  public static void main(String[] args) {
    SpringApplication.run(AWHResApp.class, args);
  }
}
