package com.foo.rest.examples.spring.bodytypes;

import com.foo.rest.examples.spring.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Created by arcuri82 on 07-Nov-18.
 */
@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class BodyTypesApplication  extends SwaggerConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(BodyTypesApplication.class, args);
    }

}
