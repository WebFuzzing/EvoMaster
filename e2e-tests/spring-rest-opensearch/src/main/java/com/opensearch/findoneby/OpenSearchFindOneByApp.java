package com.opensearch.findoneby;

import com.opensearch.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@ComponentScan("com.opensearch")
public class OpenSearchFindOneByApp extends SwaggerConfiguration {
    public OpenSearchFindOneByApp() {
        super("findoneby");
    }

    public static void main(String[] args) {
        SpringApplication.run(OpenSearchFindOneByApp.class, args);
    }

}
