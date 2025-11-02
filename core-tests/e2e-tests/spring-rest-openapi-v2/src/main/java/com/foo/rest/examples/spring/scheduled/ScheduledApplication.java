package com.foo.rest.examples.spring.scheduled;

import com.foo.rest.examples.spring.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableScheduling
@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class ScheduledApplication extends SwaggerConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(ScheduledApplication.class, args);
    }

}
