package com.foo.rest.examples.spring.filecreationissuewithfault;

import com.foo.rest.examples.spring.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class FileCreationIssueWithFaultApplication extends SwaggerConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(FileCreationIssueWithFaultApplication.class, args);
    }
}