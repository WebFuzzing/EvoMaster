package com.foo.spring.rest.h2.columntypes;

import com.foo.spring.rest.h2.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class H2ColumnTypesApplication extends SwaggerConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(H2ColumnTypesApplication.class, args);
    }

}
