package com.foo.asyncapi.ncs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * NCS over Kafka: the six numerical operations of the NCS case study, each consuming a request
 * topic and answering on a reply topic, as described by {@code asyncapi/ncs-kafka.yaml}.
 *
 * The service speaks only Kafka. Its one component is {@link NcsRequestConsumer}.
 */
/*
    Bean validation is excluded: this service validates nothing, and the validation API that the
    EvoMaster client puts on the classpath would otherwise make Spring look for an EL
    implementation that is not there.
 */
@SpringBootApplication(exclude = ValidationAutoConfiguration.class)
public class NcsKafkaApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(NcsKafkaApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
