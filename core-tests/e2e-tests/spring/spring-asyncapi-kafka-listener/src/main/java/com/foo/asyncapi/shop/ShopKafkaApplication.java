package com.foo.asyncapi.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A shop back end that is driven only by Kafka messages, and never answers any of them: an order
 * placed, an invoice requested, a stock adjustment, a return requested, a restock requested. Each
 * is consumed by a {@code @KafkaListener}, with Spring Kafka's {@code JsonDeserializer} turning the
 * record into a typed object, as described by {@code asyncapi/shop-kafka.yaml}, and restocking by
 * {@code asyncapi/shop-restock-kafka.yaml}.
 *
 * Written for the EvoMaster E2E tests. Each listener is there for one thing a search has to cope
 * with when nothing comes back from the service: {@link OrderListener} hides code behind string
 * values random data does not produce, {@link InvoiceListener} leaves its work to another thread,
 * {@link StockListener} keeps state in a database from one message to the next,
 * {@link ReturnListener} reads a payload the document does not describe at all, and
 * {@link RestockListener} calls an HTTP service that is not there.
 */
/*
    Bean validation is excluded: this service validates nothing, and the validation API that the
    EvoMaster client puts on the classpath would otherwise make Spring look for an EL
    implementation that is not there.
 */
@SpringBootApplication(exclude = ValidationAutoConfiguration.class)
public class ShopKafkaApplication {

    /**
     * Where {@link InvoiceListener} hands its work to. One thread, so that invoices are rendered
     * in the order they were requested.
     */
    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService invoiceExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(ShopKafkaApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
