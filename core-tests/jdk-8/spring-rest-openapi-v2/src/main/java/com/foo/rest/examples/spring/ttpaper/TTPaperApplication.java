package com.foo.rest.examples.spring.ttpaper;


import com.foo.rest.examples.spring.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class TTPaperApplication extends SwaggerConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(TTPaperApplication.class, args);
    }
}
