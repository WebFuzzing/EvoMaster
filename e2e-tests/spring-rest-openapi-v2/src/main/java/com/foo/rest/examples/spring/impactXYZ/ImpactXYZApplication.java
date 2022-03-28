package com.foo.rest.examples.spring.impactXYZ;

import com.foo.rest.examples.spring.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * created by manzh on 2019-09-12
 */
@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class ImpactXYZApplication extends SwaggerConfiguration {

    public static void main(String[] args){
        SpringApplication.run(ImpactXYZApplication.class, args);
    }


}