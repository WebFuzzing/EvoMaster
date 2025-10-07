package com.opensearch.queries;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@ComponentScan(basePackages = {"com.opensearch"})
public class OpenSearchQueriesApp {
    public static void main(String[] args) {
        SpringApplication.run(OpenSearchQueriesApp.class, args);
    }
}
