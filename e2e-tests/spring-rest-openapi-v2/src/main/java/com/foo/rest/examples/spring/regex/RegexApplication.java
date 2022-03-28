package com.foo.rest.examples.spring.regex;

import com.foo.rest.examples.spring.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Created by arcuri82 on 11-Jun-19.
 */
@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@RestController
public class RegexApplication  extends SwaggerConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(RegexApplication.class, args);
    }


    @GetMapping(path = "/api/{value:\\d{4}-\\d{2}-\\d{2}}")
    public String get(
            @PathVariable("value") String value
    ){
        return value;
    }
}
