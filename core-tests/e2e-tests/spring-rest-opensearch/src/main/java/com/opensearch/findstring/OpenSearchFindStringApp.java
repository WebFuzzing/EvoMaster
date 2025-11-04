package com.opensearch.findstring;

import com.opensearch.config.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@ComponentScan({"com.opensearch.config", "com.opensearch.findstring"})
public class OpenSearchFindStringApp extends SwaggerConfiguration {
    public OpenSearchFindStringApp() {
        super("findstring");
    }

    public static void main(String[] args) {
        SpringApplication.run(OpenSearchFindStringApp.class, args);
    }

}

