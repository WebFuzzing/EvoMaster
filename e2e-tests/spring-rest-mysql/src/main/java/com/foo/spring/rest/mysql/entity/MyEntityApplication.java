package com.foo.spring.rest.mysql.entity;

import com.foo.spring.rest.mysql.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class MyEntityApplication extends SwaggerConfiguration {
    public static void main(String[] args) {
        SpringApplication.run(MyEntityApplication.class, args);
    }
}
