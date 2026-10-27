package com.foo.asyncapi.shop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Consumes {@link RestockRequested}, and asks the supplier over HTTP whether it can deliver.
 *
 * The supplier is at a hostname that does not resolve, so without a mock every call fails, and
 * neither {@link RestockConfirmed} nor {@link RestockRefused} is reached. What the supplier
 * answers decides between the two, so reaching the first needs the mock to answer 200.
 */
@Component
public class RestockListener {

    private static final Logger log = LoggerFactory.getLogger(RestockListener.class);

    private static final String VALUE_TYPE =
            JsonDeserializer.VALUE_DEFAULT_TYPE + "=com.foo.asyncapi.shop.RestockRequested";

    /**
     * Where the supplier is. Deliberately a hostname that resolves nowhere.
     */
    static final String SUPPLIER = "http://supplier.test:8080/api/stock/";

    /**
     * Short, so that a call to an address nothing listens on gives up quickly.
     */
    private static final int TIMEOUT_MS = 2_000;

    private static final int OK = 200;

    @KafkaListener(id = ShopTopics.RESTOCK_LISTENER, topics = ShopTopics.RESTOCK_REQUESTED, properties = VALUE_TYPE)
    public void onRestockRequested(RestockRequested request) {

        if (request.getSku() == null || request.getQuantity() == null) {
            //nothing to restock
            return;
        }

        int status;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(SUPPLIER + request.getSku()).openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            try {
                status = connection.getResponseCode();
            } finally {
                connection.disconnect();
            }
        } catch (IOException e) {
            //the supplier could not be reached: the restock is left for later
            log.debug("Supplier unreachable: {}", e.getMessage());
            return;
        }

        if (status == OK) {
            RestockConfirmed.record(request.getSku(), request.getQuantity());
        } else {
            RestockRefused.record(request.getSku(), status);
        }
    }
}
