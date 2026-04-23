package com.mongo.objectid;

import com.mongo.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class MongoObjectIdApp extends SwaggerConfiguration {
    public MongoObjectIdApp() {
        super("objectid");
    }

    public static void main(String[] args) {
        SpringApplication.run(MongoObjectIdApp.class, args);
    }

}