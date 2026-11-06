package com.foo.asyncapi.kafka.listener;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A service that consumes Kafka messages with a {@code @KafkaListener} and never answers, as
 * described by {@code asyncapi/listener-kafka.yaml}.
 */
//without the exclusion, the validation API from the EvoMaster client makes Spring look for a missing EL implementation
@SpringBootApplication(exclude = ValidationAutoConfiguration.class)
@PropertySource("classpath:listener-kafka.properties")
public class ListenerKafkaApplication {

    /**
     * Where {@link OrderListener} hands its work to.
     */
    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService dispatchExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(ListenerKafkaApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
