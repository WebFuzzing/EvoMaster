package com.foo.rest.examples.spring.security.accesscontrol.deleteput;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication
public class SimpleAccessControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleAccessControlApplication.class, args);
    }
}