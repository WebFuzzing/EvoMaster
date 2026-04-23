package com.opensearch.age;

import com.opensearch.config.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@ComponentScan({"com.opensearch.config", "com.opensearch.age"})
public class OpenSearchAgeApp extends SwaggerConfiguration {
    public OpenSearchAgeApp() {
        super("age");
    }

    public static void main(String[] args) {
        SpringApplication.run(com.opensearch.age.OpenSearchAgeApp.class, args);
    }

}
