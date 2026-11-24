package com.foo.asyncapi.kafka.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What the service tells the rest of the world about an order, published with Spring Kafka's
 * {@code KafkaTemplate} as JSON text.
 *
 * The confirmation goes to a topic {@code asyncapi/listener-kafka.yaml} declares for the service
 * to send to. The audit record goes to one the document does not mention at all: a deliberate
 * defect, standing for the event a service publishes and its contract forgot.
 */
@Component
public class OrderEvents {

    /**
     * Where every order taken is confirmed, as the document declares.
     */
    public static final String ORDER_CONFIRMED = "order.confirmed";

    /**
     * Where every order taken is audited. Not in the document, deliberately.
     */
    public static final String ORDER_AUDIT = "order.audit";

    static final String CONFIRMED = "CONFIRMED";

    private final ObjectMapper mapper = new ObjectMapper();

    private final KafkaTemplate<String, String> kafka;

    public OrderEvents(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    void confirmed(OrderPlaced order) {

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("status", CONFIRMED);
        event.put("shipping", order.getShipping());

        kafka.send(ORDER_CONFIRMED, json(event));
    }

    void audited(OrderPlaced order) {

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("shipping", order.getShipping());

        kafka.send(ORDER_AUDIT, json(event));
    }

    private String json(Map<String, Object> event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot write " + event, e);
        }
    }
}
