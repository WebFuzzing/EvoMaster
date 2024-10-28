package com.foo.rest.emb.json.signalserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class SignalServerExampleApplication {

    /**
     * This application not yet completed in EMB.
     */
    public static void main(String[] args) {
        SpringApplication.run(SignalServerExampleApplication.class, args);
    }
}
