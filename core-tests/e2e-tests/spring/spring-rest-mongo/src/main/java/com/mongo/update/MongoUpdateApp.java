package com.mongo.update;

import com.mongo.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class MongoUpdateApp extends SwaggerConfiguration {
    public MongoUpdateApp() {
        super("mongoupdate");
    }

    public static void main(String[] args) {
        SpringApplication.run(MongoUpdateApp.class, args);
    }

}
