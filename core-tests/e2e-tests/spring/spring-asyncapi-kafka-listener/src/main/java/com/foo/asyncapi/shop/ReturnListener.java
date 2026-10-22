package com.foo.asyncapi.shop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link ReturnRequested}, whose payload the document does not describe. Reaching the
 * refund takes learning the payload's shape from the class the service reads it into, and then
 * the free-text {@code reason} the service compares with.
 */
@Component
public class ReturnListener {

    private static final Logger log = LoggerFactory.getLogger(ReturnListener.class);

    /**
     * The one reason that gets a return refunded at once.
     */
    static final String DAMAGED = "DAMAGED";

    private static final String VALUE_TYPE =
            JsonDeserializer.VALUE_DEFAULT_TYPE + "=com.foo.asyncapi.shop.ReturnRequested";

    @KafkaListener(id = ShopTopics.RETURNS_LISTENER, topics = ShopTopics.RETURNS_REQUESTED, properties = VALUE_TYPE)
    public void onReturnRequested(ReturnRequested request) {

        if (DAMAGED.equals(request.getReason())) {
            DamagedReturn.refund(request);
        }

        log.debug("Return of order {} requested", request.getOrderId());
    }
}
