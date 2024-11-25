package com.foo.rest.emb.json.tiltaksgjennomforing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, ErrorMvcAutoConfiguration.class})
public class TiltaksgjennomforingExampleApplication {

    /**
     * This application not yet completed in EMB.
     */
    public static void main(String[] args) {
        SpringApplication.run(TiltaksgjennomforingExampleApplication.class, args);
    }
}