package com.foo.asyncapi.shop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link OrderPlaced}. Two things it does are behind a string value that the contract
 * does not list, so that a search drawing random strings does not get there, but one that sees
 * what the service compares the value with does: express shipping, and an urgent priority.
 */
@Component
public class OrderListener {

    private static final Logger log = LoggerFactory.getLogger(OrderListener.class);

    /**
     * Compared with {@code equals}, the way a string-keyed rule usually is.
     */
    static final String EXPRESS = "EXPRESS";

    /**
     * Which class the record value is read into, for this listener only: each topic carries a
     * type of its own, and the publisher sends no type header.
     */
    private static final String VALUE_TYPE =
            JsonDeserializer.VALUE_DEFAULT_TYPE + "=com.foo.asyncapi.shop.OrderPlaced";

    @KafkaListener(id = ShopTopics.ORDERS_LISTENER, topics = ShopTopics.ORDERS_PLACED, properties = VALUE_TYPE)
    public void onOrderPlaced(OrderPlaced order) {

        if (EXPRESS.equals(order.getShipping())) {
            ExpressDispatch.book(order);
        }

        Priority priority = priorityOf(order);
        if (priority == Priority.URGENT) {
            UrgentEscalation.escalate(order);
        }

        log.debug("Order {} placed", order.getOrderId());
    }

    /**
     * The order's priority, or null when it names none of them: an order is still taken when its
     * priority is not understood.
     */
    private static Priority priorityOf(OrderPlaced order) {

        if (order.getPriority() == null) {
            return null;
        }
        try {
            return Priority.valueOf(order.getPriority());
        } catch (IllegalArgumentException e) {
            log.debug("Unknown priority {}", order.getPriority());
            return null;
        }
    }
}
