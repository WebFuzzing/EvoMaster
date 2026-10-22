package com.foo.asyncapi.shop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What the shop tells the rest of the world, published with Spring Kafka's {@code KafkaTemplate}
 * as JSON text.
 *
 * An order confirmed goes to a topic {@code asyncapi/shop-kafka.yaml} declares for the service to
 * send to. A change of stock goes to one the document does not mention at all: a deliberate
 * defect, standing for the event a service publishes and its contract forgot.
 */
@Component
public class ShopEvents {

    static final String CONFIRMED = "CONFIRMED";

    private static final String ORDER_ID = "orderId";

    private static final String STATUS = "status";

    private static final String QUANTITY = "quantity";

    private static final String SKU = "sku";

    private static final String DELTA = "delta";

    private final ObjectMapper mapper = new ObjectMapper();

    private final KafkaTemplate<String, String> kafka;

    public ShopEvents(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    /**
     * That an order was taken, keyed by the order, so that the events of one order stay in
     * order.
     */
    void orderConfirmed(OrderPlaced order) {

        Map<String, Object> event = new LinkedHashMap<>();
        event.put(ORDER_ID, order.getOrderId());
        event.put(STATUS, CONFIRMED);
        event.put(QUANTITY, order.getQuantity());

        kafka.send(ShopTopics.ORDERS_CONFIRMED, order.getOrderId(), json(event));
    }

    /**
     * That the stock of a product changed. Deliberately published to a topic the document does
     * not declare.
     */
    void stockChanged(String sku, int delta) {

        Map<String, Object> event = new LinkedHashMap<>();
        event.put(SKU, sku);
        event.put(DELTA, delta);

        kafka.send(ShopTopics.STOCK_CHANGES, sku, json(event));
    }

    private String json(Map<String, Object> event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot write " + event, e);
        }
    }
}
