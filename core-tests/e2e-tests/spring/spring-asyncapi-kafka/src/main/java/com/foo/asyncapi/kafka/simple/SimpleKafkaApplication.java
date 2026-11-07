package com.foo.asyncapi.kafka.simple;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * A service that speaks only Kafka, as described by {@code asyncapi/simple-kafka.yaml}.
 */
//without the exclusion, the validation API from the EvoMaster client makes Spring look for a missing EL implementation
@SpringBootApplication(exclude = ValidationAutoConfiguration.class)
public class SimpleKafkaApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SimpleKafkaApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
