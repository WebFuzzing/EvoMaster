package com.mongo.findoneanddelete;

import com.mongo.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class MongoFindOneAndDeleteApp extends SwaggerConfiguration {
    public MongoFindOneAndDeleteApp() {
        super("mongofindoneanddelete");
    }

    public static void main(String[] args) {
        SpringApplication.run(MongoFindOneAndDeleteApp.class, args);
    }

}
