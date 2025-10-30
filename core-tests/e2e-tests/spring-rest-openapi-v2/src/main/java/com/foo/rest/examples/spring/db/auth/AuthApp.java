package com.foo.rest.examples.spring.db.auth;

import com.foo.rest.examples.spring.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication
public class AuthApp extends SwaggerConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(AuthApp.class, args);
    }

}