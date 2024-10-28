package com.foo.rest.emb.json.restcountries;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class RestCountriesExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestCountriesExampleApplication.class, args);
    }
}
