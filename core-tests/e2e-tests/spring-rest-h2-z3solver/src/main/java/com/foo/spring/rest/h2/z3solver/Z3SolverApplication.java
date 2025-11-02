package com.foo.spring.rest.h2.z3solver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class Z3SolverApplication extends SwaggerConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(Z3SolverApplication.class, args);
    }

}
