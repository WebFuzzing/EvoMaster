package com.mongo.template;

import com.mongo.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class MongoTemplateApp extends SwaggerConfiguration {
    public MongoTemplateApp() {
        super("mongotemplate");
    }

    public static void main(String[] args) {
        SpringApplication.run(MongoTemplateApp.class, args);
    }

}
