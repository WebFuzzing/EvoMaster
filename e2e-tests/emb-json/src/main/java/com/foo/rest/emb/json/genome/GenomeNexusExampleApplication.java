package com.foo.rest.emb.json.genome;

import com.foo.rest.emb.json.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, ErrorMvcAutoConfiguration.class})
public class GenomeNexusExampleApplication extends SwaggerConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(GenomeNexusExampleApplication.class, args);
    }

}
